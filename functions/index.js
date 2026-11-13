const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");
const { google } = require("googleapis");
const { payWeeklyRewards } = require("./weeklyPayout");
const { payoutsStarted, FIRST_PAYOUT_WEEK } = require("./weeklyRewards");

admin.initializeApp();
const db = admin.firestore();
const messaging = admin.messaging();

// No public HTTP (onRequest) endpoints on purpose. Admin chores (app update config, backfills)
// run from functions/scripts/ with the developer's own Google login, so there is no URL or
// shared secret on the internet that could rewrite leaderboard/config or the leaderboard.

// ── Google Play Developer API Setup ──────────────────────────────────────────
const auth = new google.auth.GoogleAuth({
  scopes: ["https://www.googleapis.com/auth/androidpublisher"],
});

async function getPublisherClient() {
  const authClient = await auth.getClient();
  return google.androidpublisher({
    version: "v3",
    auth: authClient,
  });
}

const PACKAGE_NAME = "com.aditya1875.pokeverse.play";

// ── SUBSCRIPTION VERIFICATION ────────────────────────────────────────────────
/**
 * Verifies a purchase token with Google Play and stores/links the entitlement.
 * request.data: { purchaseToken, productId, isSubscription }
 *
 * LEGACY, keep deployed: app versions 88 up to the RevenueCat migration unlock premium ONLY when
 * this returns success, so removing it would break premium for paying users on those versions.
 * The account to link comes from the caller's verified auth, never from the request body (it
 * used to trust `uid` from the body, letting anyone write premiumState onto any account).
 */
exports.verifyPurchase = onCall(async (request) => {
  const { purchaseToken, productId, isSubscription } = request.data || {};
  const uid = request.auth?.uid ?? null;

  if (!purchaseToken || !productId) {
    throw new HttpsError("invalid-argument", "Missing purchaseToken or productId");
  }

  try {
    const publisher = await getPublisherClient();
    let expiryTimeMillis = 0;
    let purchaseState = 0;

    if (isSubscription) {
      const res = await publisher.purchases.subscriptions.get({
        packageName: PACKAGE_NAME,
        subscriptionId: productId,
        token: purchaseToken,
      });
      expiryTimeMillis = parseInt(res.data.expiryTimeMillis || "0", 10);
      purchaseState = res.data.paymentState === 1 ? 0 : 1; // 1: Purchased, 0: Pending/Other
    } else {
      const res = await publisher.purchases.products.get({
        packageName: PACKAGE_NAME,
        productId: productId,
        token: purchaseToken,
      });
      purchaseState = res.data.purchaseState;
      // For one-time products (Lifetime), we treat them as non-expiring
      expiryTimeMillis = purchaseState === 0 ? Number.MAX_SAFE_INTEGER : 0;
    }

    if (purchaseState !== 0) {
      return { success: false, error: "Purchase not in valid state" };
    }

    const now = Date.now();
    const isValid = expiryTimeMillis > now;

    if (isValid) {
      const entitlement = {
        purchaseToken,
        productId,
        expiryTimeMillis,
        isSubscription,
        lastVerified: now,
        uid: uid || null,
      };

      // Store in entitlements collection
      await db.collection("entitlements").doc(purchaseToken).set(entitlement, { merge: true });

      // Link to user if provided
      if (uid) {
        await db.collection("users").doc(uid).set(
          {
            premiumState: {
              active: true,
              plan: productId,
              expiry: expiryTimeMillis,
            },
          },
          { merge: true }
        );
      }

      return { success: true, entitlement };
    } else {
      return { success: false, error: "Purchase expired" };
    }
  } catch (error) {
    console.error("Verification failed:", error);
    throw new HttpsError("internal", "Verification failed", error.message);
  }
});

// ── Shared: build and write last-week snapshot ────────────────────────────────
// Returns { top10, weekOf } so callers can decide what else to do.
async function writeWeeklySnapshot(docs, now) {
  const weekOf = now;
  const activeDocs = docs.filter((doc) => (doc.data().weeklyXp || 0) > 0);
  const top10 = activeDocs.slice(0, 10).map((doc, idx) => {
    const d = doc.data();
    return {
      uid: d.uid || doc.id,
      displayName: d.displayName || "Trainer",
      photoUrl: d.photoUrl || "",
      weeklyXp: d.weeklyXp || 0,
      level: d.level || 1,
      rank: idx + 1,
      // The app already reads these; they were missing, so Last Week showed no bio / flair.
      bio: d.bio || "",
      isPremium: d.isPremium === true,
    };
  });

  console.log(
    `📸 Writing snapshot: ${top10.length} active trainers out of ${docs.length} total.`
  );

  await db.collection("leaderboard_meta").doc("last_week_snapshot").set({
    weekOf,
    entries: top10,
  });

  return { top10, weekOf };
}

// ── DAILY DUEL CHALLENGE SEEDER (every day 12:00 AM IST) ─────────────────────
// Creates daily_challenges/{YYYY-MM-DD} with a random seed so all clients pick
// identical questions. Safe to re-run (SetOptions.merge keeps existing seed).
exports.seedDailyChallenge = onSchedule(
  { schedule: "0 0 * * *", timeZone: "Asia/Kolkata" },
  async () => {
    const now = new Date();
    const pad = (n) => String(n).padStart(2, "0");
    const date = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;

    const ref = db.collection("daily_challenges").doc(date);
    const existing = await ref.get();
    if (existing.exists) {
      console.log(`✅ Challenge for ${date} already exists, skipping.`);
      return;
    }

    const seed = Math.floor(Math.random() * Number.MAX_SAFE_INTEGER);
    await ref.set({ seed, createdAt: Date.now() });
    console.log(`✅ Seeded daily challenge for ${date} with seed ${seed}`);
  }
);

// ── WEEKLY RESET (every Monday 12:00 AM IST) ─────────────────────────────────
// Runs automatically every week. Snapshot is written first, THEN XP is reset.
exports.resetWeeklyXp = onSchedule(
  { schedule: "0 0 * * 1", timeZone: "Asia/Kolkata" },
  async () => {
    try {
      // Single orderBy → auto single-field index, no composite index needed
      const snapshot = await db
        .collection("leaderboard")
        .orderBy("weeklyXp", "desc")
        .get();

      const docs = snapshot.docs;
      const now = Date.now();

      // ── 1. Snapshot BEFORE reset ──────────────────────────────────────────
      const { top10 } = await writeWeeklySnapshot(docs, now);

      // ── 1b. Placement coins. Best-effort: a failure here must never block the reset below. ──
      let payouts = new Map();
      try {
        if (payoutsStarted(now)) {
          payouts = (await payWeeklyRewards(db, admin.firestore.FieldValue, docs, now)).byUid;
        } else {
          console.log(`🪙 Weekly coins: not paid yet, the first payout is on ${FIRST_PAYOUT_WEEK}.`);
        }
      } catch (e) {
        console.error("❌ Weekly coin payout failed (reset continues):", e);
      }

      // ── 2. Inbox messages for top-10 ─────────────────────────────────────
      const inboxMessages = top10.map((entry, idx) => {
        const rank = idx + 1;
        let title, body, emoji;
        if (rank === 1) {
          title = "Champion! 👑";
          body = `You finished #1 last week, ${entry.displayName.split(" ")[0]}! Absolute dominance.`;
          emoji = "🏆";
        } else if (rank === 2) {
          title = "Silver Finish! 🥈";
          body = `You finished #2 last week, ${entry.displayName.split(" ")[0]}! So close to the crown.`;
          emoji = "🥈";
        } else if (rank === 3) {
          title = "Bronze Finish! 🥉";
          body = `You finished #3 last week, ${entry.displayName.split(" ")[0]}! Solid performance.`;
          emoji = "🥉";
        } else {
          title = "Top 10 Trainer! ⭐";
          body = `You finished #${rank} last week, ${entry.displayName.split(" ")[0]}! You're in the elite.`;
          emoji = "⭐";
        }
        const won = payouts.get(entry.uid);
        if (won) body = `${body} +${won.coins} Dex Coins.`;
        const msgRef = db
          .collection("user_messages")
          .doc(entry.uid)
          .collection("inbox")
          .doc();
        return {
          ref: msgRef,
          data: {
            id: msgRef.id,
            title,
            body,
            emoji,
            type: "weekly_rank",
            timestamp: now,
            isRead: false,
          },
          uid: entry.uid,
          fcmTitle: title,
          fcmBody: body,
        };
      });

      const msgBatch = db.batch();
      inboxMessages.forEach(({ ref, data }) => msgBatch.set(ref, data));
      await msgBatch.commit();

      // ── 3. FCM push (best-effort) ─────────────────────────────────────────
      for (const { uid, fcmTitle, fcmBody } of inboxMessages) {
        try {
          const userDoc = await db.collection("users").doc(uid).get();
          const fcmToken = userDoc.data()?.fcmToken;
          if (fcmToken) {
            await messaging.send({
              token: fcmToken,
              data: { title: fcmTitle, body: fcmBody, type: "weekly_rank" },
            });
          }
        } catch (e) {
          console.warn(`FCM send failed for uid ${uid}:`, e.message);
        }
      }

      // ── 4. Reset weeklyXp for ALL users ──────────────────────────────────
      const chunkSize = 400;
      let globalRank = 1;

      for (let i = 0; i < docs.length; i += chunkSize) {
        const batch = db.batch();
        docs.slice(i, i + chunkSize).forEach((doc) => {
          batch.update(doc.ref, {
            previousRank: globalRank++,
            weeklyXp: 0,
            weeklyActive: false,
            lastWeeklyReset: admin.firestore.FieldValue.serverTimestamp(),
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
          });
        });
        await batch.commit();
      }

      console.log(
        `✅ Weekly reset complete. Snapshot: ${top10.length} trainers. ` +
        `Inbox messages sent. All ${docs.length} leaderboard docs reset.`
      );
    } catch (e) {
      console.error("❌ Weekly reset failed:", e);
    }
  }
);

// ── CARD CLASH: sweep abandoned matches (every 5 minutes) ────────────────────
// Backstop for CardClashRepositoryImpl's client-side staleness filter. That filter
// only stops *new* players from matching into a dead room, it doesn't remove the
// dead room itself. Without this, every abandoned "waiting" doc (app killed,
// backgrounded, crashed, anything that skips the cancelWait() cleanup path)
// stays in the collection forever, and the collection only grows.
//
// Two passes:
//  1. "waiting" rooms older than 2 minutes, well past the 18s bot-fallback +
//     25s client staleness window, so these are unambiguously dead.
//  2. ANY match (regardless of status) not updated in 24 hours, catches games
//     abandoned mid-round (both players disconnected, so no one was left to call
//     forfeitOpponent()) and finished/cancelled docs we don't need to keep.
exports.cleanupStaleCardBattles = onSchedule({ schedule: "every 5 minutes" }, async () => {
  const now = Date.now();
  const matches = db.collection("card_battles");

  const staleWaitingCutoffMs = now - 2 * 60 * 1000;
  const longAbandonedCutoff = admin.firestore.Timestamp.fromMillis(now - 24 * 60 * 60 * 1000);

  // Equality-only filter (waiting rooms are always a small slice of the collection)
  // deliberately not adding "createdAt <" to this same query. Firestore would need a
  // composite index for that combo, and this project doesn't track firestore.indexes.json
  // (see the "No orderBy" comments elsewhere in this file), filtering the small waiting
  // subset in JS avoids that dependency entirely.
  const [waitingSnapshot, longAbandoned] = await Promise.all([
    matches.where("status", "==", "waiting").get(),
    // Single inequality filter → auto single-field index, same as the orderBy queries above.
    matches.where("lastUpdated", "<", longAbandonedCutoff).get(),
  ]);
  const staleWaiting = waitingSnapshot.docs.filter((doc) => {
    const createdAtMs = doc.get("createdAt")?.toMillis?.() ?? 0;
    return createdAtMs < staleWaitingCutoffMs;
  });

  // Dedupe, a doc can show up in both queries.
  const toDelete = new Map();
  staleWaiting.forEach((doc) => toDelete.set(doc.id, doc.ref));
  longAbandoned.docs.forEach((doc) => toDelete.set(doc.id, doc.ref));

  if (toDelete.size === 0) {
    console.log("🧹 Card Clash cleanup: nothing to remove.");
    return;
  }

  const refs = Array.from(toDelete.values());
  const chunkSize = 400; // Firestore batch write limit
  for (let i = 0; i < refs.length; i += chunkSize) {
    const batch = db.batch();
    refs.slice(i, i + chunkSize).forEach((ref) => batch.delete(ref));
    await batch.commit();
  }

  console.log(
    `🧹 Card Clash cleanup: removed ${toDelete.size} matches ` +
    `(${staleWaiting.length} stale-waiting, ${longAbandoned.size} long-abandoned).`
  );
});

/**
 * Online Team Battle rooms (team_battles/{id}) are short-lived. Sweep rooms nobody ever joined, and anything
 * that has not been touched for a day. A room has teams/ and actions/ subcollections, so it is removed with
 * recursiveDelete rather than a plain delete (which would orphan them).
 */
exports.cleanupStaleTeamBattles = onSchedule({ schedule: "every 5 minutes" }, async () => {
  const now = Date.now();
  const rooms = db.collection("team_battles");

  // A quick match waits 30s, a friend room up to 10 minutes. Two hours is far past either.
  const staleWaitingCutoffMs = now - 2 * 60 * 60 * 1000;
  const longAbandonedCutoff = admin.firestore.Timestamp.fromMillis(now - 24 * 60 * 60 * 1000);

  // Equality filter only for the waiting rooms (no composite index needed); age is checked in JS.
  const [waitingSnapshot, longAbandoned] = await Promise.all([
    rooms.where("status", "==", "waiting").get(),
    rooms.where("lastUpdated", "<", longAbandonedCutoff).get(),
  ]);
  const staleWaiting = waitingSnapshot.docs.filter((doc) => {
    const createdAtMs = doc.get("createdAt")?.toMillis?.() ?? 0;
    return createdAtMs < staleWaitingCutoffMs;
  });

  const toDelete = new Map();
  staleWaiting.forEach((doc) => toDelete.set(doc.id, doc.ref));
  longAbandoned.docs.forEach((doc) => toDelete.set(doc.id, doc.ref));

  if (toDelete.size === 0) {
    console.log("Team Battle cleanup: nothing to remove.");
    return;
  }
  for (const ref of toDelete.values()) {
    await db.recursiveDelete(ref);
  }
  console.log(
    `Team Battle cleanup: removed ${toDelete.size} rooms ` +
    `(${staleWaiting.length} stale-waiting, ${longAbandoned.size} long-abandoned).`
  );
});

// ── FEEDBACK ─────────────────────────────────────────────────────────────────
// "What should we build next?" form. Open to guests too (no auth required);
// inputs are whitelisted/capped so the collection stays clean.
const FEEDBACK_WISHES = new Set([
  "more_games", "multiplayer", "team_builder", "shiny_hunting", "pokemon_go",
  "competitive", "regional_dex", "more_languages", "widgets", "other",
]);

exports.submitFeedback = onCall(async (request) => {
  const data = request.data || {};
  const wishes = Array.isArray(data.wishes) ?
    [...new Set(data.wishes.filter((w) => FEEDBACK_WISHES.has(w)))] : [];
  const message = typeof data.message === "string" ? data.message.trim().slice(0, 1000) : "";

  if (wishes.length === 0 && message.length === 0) {
    throw new HttpsError("invalid-argument", "Pick at least one option or write a message");
  }

  await db.collection("feedback").add({
    uid: request.auth?.uid ?? null,
    wishes,
    message,
    appVersion: String(data.appVersion ?? "").slice(0, 20),
    locale: String(data.locale ?? "").slice(0, 12),
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });
  return { ok: true };
});

// ── REFERRALS ────────────────────────────────────────────────────────────────
// Server-side bookkeeping only: XP itself stays client-authoritative (like every
// other award), so the server hands out *counts* the client then turns into XP.
//   referral_codes/{CODE}    → { uid }
//   referral_profiles/{uid}  → { code, joinedCount, claimedCount, referredBy, redeemedAt }
// Clients never write these collections; all access goes through these functions.
const REFERRAL_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I
const REFERRAL_CODE_LENGTH = 6;
const REFERRAL_MAX_REWARDED = 10; // friends that earn the referrer XP
const REFERRAL_WINDOW_DAYS = 14; // a new account can enter a code for this long

function randomReferralCode() {
  let code = "";
  for (let i = 0; i < REFERRAL_CODE_LENGTH; i++) {
    code += REFERRAL_ALPHABET[Math.floor(Math.random() * REFERRAL_ALPHABET.length)];
  }
  return code;
}

function requireUid(request) {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Sign in to use referrals");
  return uid;
}

exports.getReferralCode = onCall(async (request) => {
  const uid = requireUid(request);
  const profileRef = db.collection("referral_profiles").doc(uid);
  const profile = await profileRef.get();

  if (profile.exists && profile.get("code")) {
    return {
      code: profile.get("code"),
      joined: profile.get("joinedCount") ?? 0,
      redeemed: Boolean(profile.get("referredBy")),
    };
  }

  for (let attempt = 0; attempt < 6; attempt++) {
    const code = randomReferralCode();
    const codeRef = db.collection("referral_codes").doc(code);
    try {
      await db.runTransaction(async (tx) => {
        const taken = await tx.get(codeRef);
        if (taken.exists) throw new Error("code-taken");
        tx.create(codeRef, { uid, createdAt: admin.firestore.FieldValue.serverTimestamp() });
        tx.set(profileRef, { code }, { merge: true });
      });
      return { code, joined: profile.get("joinedCount") ?? 0, redeemed: Boolean(profile.get("referredBy")) };
    } catch (e) {
      if (e.message !== "code-taken") throw e;
    }
  }
  throw new HttpsError("internal", "Could not allocate a referral code");
});

exports.redeemReferral = onCall(async (request) => {
  const uid = requireUid(request);
  const code = String(request.data?.code ?? "").trim().toUpperCase();
  if (!/^[A-Z0-9]{4,10}$/.test(code)) throw new HttpsError("invalid-argument", "bad-code");

  // Only new accounts can be referred, stops existing players swapping codes.
  const user = await admin.auth().getUser(uid);
  const createdMs = Date.parse(user.metadata.creationTime);
  if (Date.now() - createdMs > REFERRAL_WINDOW_DAYS * 24 * 60 * 60 * 1000) {
    throw new HttpsError("failed-precondition", "too-late");
  }

  const codeDoc = await db.collection("referral_codes").doc(code).get();
  if (!codeDoc.exists) throw new HttpsError("not-found", "unknown-code");
  const referrerUid = codeDoc.get("uid");
  if (referrerUid === uid) throw new HttpsError("invalid-argument", "own-code");

  const refereeRef = db.collection("referral_profiles").doc(uid);
  const referrerRef = db.collection("referral_profiles").doc(referrerUid);

  await db.runTransaction(async (tx) => {
    const [referee, referrer] = await Promise.all([tx.get(refereeRef), tx.get(referrerRef)]);
    if (referee.get("referredBy")) throw new HttpsError("already-exists", "already-redeemed");
    // No mutual referrals: A invites B, B can't then "refer" A back.
    if (referrer.get("referredBy") === uid) throw new HttpsError("invalid-argument", "mutual");

    tx.set(refereeRef, {
      referredBy: referrerUid,
      redeemedCode: code,
      redeemedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
    tx.set(referrerRef, {
      joinedCount: admin.firestore.FieldValue.increment(1),
    }, { merge: true });
  });

  return { ok: true };
});

/** Returns how many newly-joined friends the caller hasn't been paid for yet, and marks them paid. */
exports.claimReferralRewards = onCall(async (request) => {
  const uid = requireUid(request);
  const ref = db.collection("referral_profiles").doc(uid);
  return db.runTransaction(async (tx) => {
    const doc = await tx.get(ref);
    const joined = doc.get("joinedCount") ?? 0;
    const claimed = doc.get("claimedCount") ?? 0;
    const rewardable = Math.min(joined, REFERRAL_MAX_REWARDED);
    const newly = Math.max(0, rewardable - claimed);
    if (newly > 0) tx.set(ref, { claimedCount: rewardable }, { merge: true });
    return { newly, joined };
  });
});
