// Security rules tests. `npm test` boots the Firestore + Storage emulators and runs this file.
// Each "app:" test replays a write the shipped app really makes (it must keep working);
// each "attack:" test is something a modified client or a script could try (it must fail).
const { test, before, after, beforeEach } = require("node:test");
const fs = require("fs");
const path = require("path");
const {
  initializeTestEnvironment, assertSucceeds, assertFails,
} = require("@firebase/rules-unit-testing");
const {
  doc, setDoc, updateDoc, getDoc, deleteDoc, getDocs, collection, query, where, limit,
  Timestamp, runTransaction,
} = require("firebase/firestore");
const { ref, uploadBytes, deleteObject } = require("firebase/storage");

let env;
const ALICE = "aliceUid123";
const BOB = "bobUid456";
const EVE = "eveUid789";
const now = () => Timestamp.now();

// Exactly what UserProfileRepository.syncToFirestore writes to leaderboard/{uid}.
function leaderboardDoc(uid, over = {}) {
  return {
    uid, displayName: "Ash", displayNameLower: "ash", bio: "Gotta catch em all",
    photoUrl: "https://lh3.googleusercontent.com/a/abc=s96-c",
    totalXp: 1200, weeklyXp: 300, lastWeeklyReset: 1761523200000, level: 5,
    updatedAt: now(), weeklyActive: true, ...over,
  };
}
// A representative subset of what syncToFirestore writes to users/{uid}.
function userDoc(uid, over = {}) {
  return {
    uid, username: "Ash", bio: "", photoUrl: "", email: "ash@example.com",
    totalXp: 1200, weeklyXp: 300, lastWeeklyReset: 1761523200000, level: 5,
    gamesPlayed: 10, dailyStreak: 3, updatedAt: now(), ...over,
  };
}
const db = (uid) => (uid ? env.authenticatedContext(uid) : env.unauthenticatedContext()).firestore();
const storage = (uid) => (uid ? env.authenticatedContext(uid) : env.unauthenticatedContext()).storage();
async function seed(p, data) {
  await env.withSecurityRulesDisabled((ctx) => setDoc(doc(ctx.firestore(), p), data));
}

before(async () => {
  env = await initializeTestEnvironment({
    projectId: "demo-dexverse",
    firestore: { rules: fs.readFileSync(path.join(__dirname, "../firestore.rules"), "utf8"), host: "127.0.0.1", port: 8080 },
    storage: { rules: fs.readFileSync(path.join(__dirname, "../storage.rules"), "utf8"), host: "127.0.0.1", port: 9199 },
  });
});
after(() => env.cleanup());
beforeEach(() => env.clearFirestore());

// ── leaderboard ──────────────────────────────────────────────────────────────
test("app: first leaderboard sync creates own doc", async () => {
  await assertSucceeds(setDoc(doc(db(ALICE), "leaderboard", ALICE), leaderboardDoc(ALICE), { merge: true }));
});
test("app: later sync with more XP (merge set)", async () => {
  await seed(`leaderboard/${ALICE}`, leaderboardDoc(ALICE));
  await assertSucceeds(setDoc(doc(db(ALICE), "leaderboard", ALICE),
      leaderboardDoc(ALICE, { totalXp: 1400, weeklyXp: 500 }), { merge: true }));
});
test("app: own uploaded Storage photo URL is allowed", async () => {
  const url = `https://firebasestorage.googleapis.com/v0/b/composepractice-33177.firebasestorage.app/o/profile_photos%2F${ALICE}.jpg?alt=media&token=x`;
  await assertSucceeds(setDoc(doc(db(ALICE), "leaderboard", ALICE), leaderboardDoc(ALICE, { photoUrl: url })));
});
test("app: stale-week sync resets weeklyXp to 0", async () => {
  await seed(`leaderboard/${ALICE}`, leaderboardDoc(ALICE, { weeklyXp: 900 }));
  await assertSucceeds(setDoc(doc(db(ALICE), "leaderboard", ALICE),
      leaderboardDoc(ALICE, { weeklyXp: 0, weeklyActive: false }), { merge: true }));
});
test("app: premium flair merge write", async () => {
  await seed(`leaderboard/${ALICE}`, leaderboardDoc(ALICE));
  await assertSucceeds(setDoc(doc(db(ALICE), "leaderboard", ALICE), { isPremium: true }, { merge: true }));
});
test("app: doc carrying server fields (previousRank, legacy empty email) still syncs", async () => {
  await seed(`leaderboard/${ALICE}`, { ...leaderboardDoc(ALICE), previousRank: 12, email: "" });
  await assertSucceeds(setDoc(doc(db(ALICE), "leaderboard", ALICE),
      leaderboardDoc(ALICE, { totalXp: 1300, email: "" }), { merge: true }));
});
test("app: lastWeeklyReset written as a server timestamp by the function is accepted", async () => {
  await seed(`leaderboard/${ALICE}`, leaderboardDoc(ALICE, { lastWeeklyReset: now() }));
  await assertSucceeds(setDoc(doc(db(ALICE), "leaderboard", ALICE), { isPremium: false }, { merge: true }));
});
test("app: signed-in player reads the board", async () => {
  await seed(`leaderboard/${BOB}`, leaderboardDoc(BOB));
  await assertSucceeds(getDoc(doc(db(ALICE), "leaderboard", BOB)));
});
test("app: GUEST can read leaderboard/config (update prompt)", async () => {
  await seed("leaderboard/config", { latestVersionCode: 96, minVersionCode: 0 });
  await assertSucceeds(getDoc(doc(db(null), "leaderboard", "config")));
});
test("attack: guest reads player docs", async () => {
  await seed(`leaderboard/${BOB}`, leaderboardDoc(BOB));
  await assertFails(getDoc(doc(db(null), "leaderboard", BOB)));
});
test("attack: write someone else's leaderboard doc", async () => {
  await assertFails(setDoc(doc(db(EVE), "leaderboard", ALICE), leaderboardDoc(ALICE)));
});
test("attack: write leaderboard/config (force-update everyone)", async () => {
  await assertFails(setDoc(doc(db(EVE), "leaderboard", "config"), { minVersionCode: 9999 }));
});
test("attack: jump XP by a million in one write", async () => {
  await seed(`leaderboard/${EVE}`, leaderboardDoc(EVE));
  await assertFails(updateDoc(doc(db(EVE), "leaderboard", EVE), { totalXp: 999999 }));
});
test("attack: create a fresh doc with huge XP", async () => {
  await assertFails(setDoc(doc(db(EVE), "leaderboard", EVE), leaderboardDoc(EVE, { totalXp: 500000, weeklyXp: 9000 })));
});
test("attack: weekly XP above a week's ceiling", async () => {
  await seed(`leaderboard/${EVE}`, leaderboardDoc(EVE, { totalXp: 90000 }));
  await assertFails(updateDoc(doc(db(EVE), "leaderboard", EVE), { weeklyXp: 20000, totalXp: 100000 }));
});
test("attack: XP as a string / float", async () => {
  await assertFails(setDoc(doc(db(EVE), "leaderboard", EVE), leaderboardDoc(EVE, { totalXp: "999999" })));
  await assertFails(setDoc(doc(db(EVE), "leaderboard", EVE), leaderboardDoc(EVE, { totalXp: 1.5e5 })));
});
test("attack: lower someone's... own XP back down (stale device wipe)", async () => {
  await seed(`leaderboard/${ALICE}`, leaderboardDoc(ALICE, { totalXp: 5000 }));
  await assertFails(updateDoc(doc(db(ALICE), "leaderboard", ALICE), { totalXp: 100 }));
});
test("attack: fake a rank arrow (previousRank is server-only)", async () => {
  await seed(`leaderboard/${EVE}`, leaderboardDoc(EVE));
  await assertFails(updateDoc(doc(db(EVE), "leaderboard", EVE), { previousRank: 1 }));
});
test("attack: publish an email publicly", async () => {
  await assertFails(setDoc(doc(db(EVE), "leaderboard", EVE), leaderboardDoc(EVE, { email: "x@y.z" })));
});
test("attack: tracking-pixel / foreign photo URL", async () => {
  await assertFails(setDoc(doc(db(EVE), "leaderboard", EVE), leaderboardDoc(EVE, { photoUrl: "https://evil.example/pixel.gif" })));
  await assertFails(setDoc(doc(db(EVE), "leaderboard", EVE), leaderboardDoc(EVE, {
    photoUrl: `https://firebasestorage.googleapis.com/v0/b/x/o/profile_photos%2F${ALICE}.jpg?alt=media` })));
  await assertFails(setDoc(doc(db(EVE), "leaderboard", EVE), leaderboardDoc(EVE, { photoUrl: "content://media/external/images/1" })));
});
test("attack: oversized name / bio, arbitrary extra fields", async () => {
  await assertFails(setDoc(doc(db(EVE), "leaderboard", EVE), leaderboardDoc(EVE, { displayName: "x".repeat(200) })));
  await assertFails(setDoc(doc(db(EVE), "leaderboard", EVE), leaderboardDoc(EVE, { bio: "x".repeat(500) })));
  await assertFails(setDoc(doc(db(EVE), "leaderboard", EVE), leaderboardDoc(EVE, { isAdmin: true })));
});
test("attack: delete own doc (to recreate around the XP bounds)", async () => {
  await seed(`leaderboard/${EVE}`, leaderboardDoc(EVE));
  await assertFails(deleteDoc(doc(db(EVE), "leaderboard", EVE)));
});

// ── users ────────────────────────────────────────────────────────────────────
test("app: users sync, best score + gamesPlayed + fcmToken writes", async () => {
  await assertSucceeds(setDoc(doc(db(ALICE), "users", ALICE), userDoc(ALICE), { merge: true }));
  await assertSucceeds(setDoc(doc(db(ALICE), "users", ALICE), { bestQuizScore: 9 }, { merge: true }));
  await assertSucceeds(setDoc(doc(db(ALICE), "users", ALICE), { gamesPlayed: 11 }, { merge: true }));
  await assertSucceeds(updateDoc(doc(db(ALICE), "users", ALICE), { fcmToken: "tok" }));
  await assertSucceeds(getDoc(doc(db(ALICE), "users", ALICE)));
});
test("live data: stale weeklyXp above the public cap doesn't block partial writes", async () => {
  // Real shape found in production: users docs aren't reset weekly by the server.
  await seed(`users/${ALICE}`, userDoc(ALICE, { weeklyXp: 37635, totalXp: 37635 }));
  await assertSucceeds(updateDoc(doc(db(ALICE), "users", ALICE), { fcmToken: "tok" }));
  await assertSucceeds(setDoc(doc(db(ALICE), "users", ALICE), { gamesPlayed: 12 }, { merge: true }));
  await assertSucceeds(setDoc(doc(db(ALICE), "users", ALICE), userDoc(ALICE, { weeklyXp: 0, totalXp: 37700 }), { merge: true }));
});
test("live data: v88 buyers (server-written premiumState) can still sync", async () => {
  await seed(`users/${ALICE}`, userDoc(ALICE, { premiumState: { active: true, plan: "x", expiry: 1 } }));
  await assertSucceeds(setDoc(doc(db(ALICE), "users", ALICE), userDoc(ALICE, { totalXp: 1300 }), { merge: true }));
});
test("attack: read another player's private profile (email, fcmToken)", async () => {
  await seed(`users/${ALICE}`, userDoc(ALICE));
  await assertFails(getDoc(doc(db(EVE), "users", ALICE)));
});
test("attack: grant yourself premiumState", async () => {
  await seed(`users/${EVE}`, userDoc(EVE));
  await assertFails(updateDoc(doc(db(EVE), "users", EVE), { premiumState: { active: true } }));
});
test("attack: inflate cloud-save XP (later copied to the board)", async () => {
  await seed(`users/${EVE}`, userDoc(EVE));
  await assertFails(updateDoc(doc(db(EVE), "users", EVE), { totalXp: 900000 }));
});

// ── card_battles ─────────────────────────────────────────────────────────────
// Exactly what CardClashRepositoryImpl.createMatch writes.
function newMatch(p1) {
  return {
    player1Id: p1, player2Id: "", player1Name: "Ash", player2Name: "", status: "waiting",
    roomCode: "ABCD", currentRound: 1, p1Score: 0.0, p2Score: 0.0, winner: null,
    player1Ready: false, player2Ready: false, roundP1Locked: false, roundP2Locked: false,
    roundP1CardId: -1, roundP2CardId: -1, roundRevealed: false, roundWinner: null,
    roundP1Score: 0.0, roundP2Score: 0.0, completedRounds: [], heartbeatP1Ms: Date.now(),
    heartbeatP2Ms: 0, createdAt: now(), lastUpdated: now(),
  };
}
const join = (uid) => ({ player2Id: uid, player2Name: "Gary", status: "dealing", lastUpdated: now() });

test("app: full Card Clash flow (create, matchmake query, join, hands, play, finish)", async () => {
  await assertSucceeds(setDoc(doc(db(ALICE), "card_battles", "m1"), newMatch(ALICE)));
  await assertSucceeds(getDocs(query(collection(db(BOB), "card_battles"),
      where("status", "==", "waiting"), where("player2Id", "==", ""), limit(10))));
  await assertSucceeds(updateDoc(doc(db(BOB), "card_battles", "m1"), join(BOB)));
  await assertSucceeds(setDoc(doc(db(ALICE), "card_battles/m1/hands", ALICE), { cards: [], usedCardIds: [] }));
  await assertSucceeds(setDoc(doc(db(BOB), "card_battles/m1/hands", BOB), { cards: [], usedCardIds: [] }));
  await assertSucceeds(updateDoc(doc(db(BOB), "card_battles", "m1"), { player2Ready: true, lastUpdated: now() }));
  await assertSucceeds(updateDoc(doc(db(ALICE), "card_battles", "m1"), { status: "active", lastUpdated: now() }));
  await assertSucceeds(updateDoc(doc(db(BOB), "card_battles", "m1"), { heartbeatP2Ms: Date.now() }));
  const alice = db(ALICE);
  await assertSucceeds(runTransaction(alice, async (tx) => {
    const r = doc(alice, "card_battles", "m1"); await tx.get(r);
    tx.update(r, { roundP1CardId: 25, lastUpdated: now() });
  }));
  await assertSucceeds(updateDoc(doc(db(BOB), "card_battles", "m1"),
      { status: "finished", winner: "player2", p1Score: 1.0, p2Score: 2.0, lastUpdated: now() }));
  await assertSucceeds(getDoc(doc(db(BOB), "card_battles", "m1")));
});
test("app: room-code join query + creator cancels unjoined room", async () => {
  await seed("card_battles/m2", newMatch(ALICE));
  await assertSucceeds(getDocs(query(collection(db(BOB), "card_battles"),
      where("roomCode", "==", "ABCD"), where("status", "==", "waiting"), limit(1))));
  await assertSucceeds(updateDoc(doc(db(ALICE), "card_battles", "m2"),
      { status: "finished", winner: "draw", p1Score: 0.0, p2Score: 0.0, lastUpdated: now() }));
});
test("attack: create a room pretending to be someone else", async () => {
  await assertFails(setDoc(doc(db(EVE), "card_battles", "m3"), newMatch(ALICE)));
});
test("attack: outsider hijacks a full match / reads it", async () => {
  await seed("card_battles/m4", { ...newMatch(ALICE), player2Id: BOB, status: "active" });
  await assertFails(updateDoc(doc(db(EVE), "card_battles", "m4"), { winner: "player2" }));
  await assertFails(getDoc(doc(db(EVE), "card_battles", "m4")));
});
test("attack: joiner rewrites the score while joining", async () => {
  await seed("card_battles/m5", newMatch(ALICE));
  await assertFails(updateDoc(doc(db(EVE), "card_battles", "m5"), { ...join(EVE), p2Score: 5.0 }));
});
test("attack: player swaps the opponent out of the match", async () => {
  await seed("card_battles/m6", { ...newMatch(ALICE), player2Id: BOB, status: "active" });
  await assertFails(updateDoc(doc(db(ALICE), "card_battles", "m6"), { player2Id: EVE }));
});
test("attack: peek at / plant the opponent's hand, or a hand in a match you're not in", async () => {
  await seed("card_battles/m7", { ...newMatch(ALICE), player2Id: BOB, status: "active" });
  await seed(`card_battles/m7/hands/${BOB}`, { cards: [], usedCardIds: [] });
  await assertFails(getDoc(doc(db(ALICE), "card_battles/m7/hands", BOB)));
  await assertFails(setDoc(doc(db(ALICE), "card_battles/m7/hands", BOB), { cards: [] }));
  await assertFails(setDoc(doc(db(EVE), "card_battles/m7/hands", EVE), { cards: [] }));
});

// ── inbox & meta ─────────────────────────────────────────────────────────────
test("app: read inbox and mark a message read", async () => {
  await seed(`user_messages/${ALICE}/inbox/msg1`, { title: "Champion!", isRead: false });
  await assertSucceeds(getDoc(doc(db(ALICE), `user_messages/${ALICE}/inbox`, "msg1")));
  await assertSucceeds(updateDoc(doc(db(ALICE), `user_messages/${ALICE}/inbox`, "msg1"), { isRead: true }));
});
test("attack: plant/edit inbox messages, read another inbox", async () => {
  await seed(`user_messages/${ALICE}/inbox/msg1`, { title: "Top 10", isRead: false });
  await assertFails(setDoc(doc(db(ALICE), `user_messages/${ALICE}/inbox`, "fake"), { title: "You won $$$" }));
  await assertFails(updateDoc(doc(db(ALICE), `user_messages/${ALICE}/inbox`, "msg1"), { title: "Champion!" }));
  await assertFails(getDoc(doc(db(EVE), `user_messages/${ALICE}/inbox`, "msg1")));
});
test("app: Last Week snapshot readable; attack: write it", async () => {
  await seed("leaderboard_meta/last_week_snapshot", { entries: [] });
  await assertSucceeds(getDoc(doc(db(ALICE), "leaderboard_meta", "last_week_snapshot")));
  await assertFails(setDoc(doc(db(ALICE), "leaderboard_meta", "last_week_snapshot"), { entries: [] }));
});
test("attack: server-only collections (referrals, feedback, entitlements)", async () => {
  await assertFails(setDoc(doc(db(EVE), "referral_profiles", EVE), { joinedCount: 99 }));
  await assertFails(getDoc(doc(db(EVE), "referral_codes", "ABC123")));
  await assertFails(setDoc(doc(db(EVE), "feedback", "x"), { message: "spam" }));
  await assertFails(getDoc(doc(db(EVE), "entitlements", "tok")));
});

// ── storage ──────────────────────────────────────────────────────────────────
const jpeg = Buffer.alloc(50 * 1024, 1);
test("app: upload own avatar (new app: image/jpeg; old app: octet-stream)", async () => {
  await assertSucceeds(uploadBytes(ref(storage(ALICE), `profile_photos/${ALICE}.jpg`), jpeg, { contentType: "image/jpeg" }));
  await assertSucceeds(uploadBytes(ref(storage(ALICE), `profile_photos/${ALICE}.jpg`), jpeg, { contentType: "application/octet-stream" }));
});
test("attack: overwrite someone's avatar, host other files, upload huge files", async () => {
  await assertFails(uploadBytes(ref(storage(EVE), `profile_photos/${ALICE}.jpg`), jpeg, { contentType: "image/jpeg" }));
  await assertFails(uploadBytes(ref(storage(EVE), `profile_photos/${EVE}.jpg`), jpeg, { contentType: "application/zip" }));
  await assertFails(uploadBytes(ref(storage(EVE), `warez/${EVE}.apk`), jpeg, { contentType: "image/jpeg" }));
  await assertFails(uploadBytes(ref(storage(EVE), `profile_photos/${EVE}.jpg`), Buffer.alloc(11 * 1024 * 1024), { contentType: "image/jpeg" }));
  await assertFails(uploadBytes(ref(storage(null), `profile_photos/anon.jpg`), jpeg, { contentType: "image/jpeg" }));
});
test("attack: delete someone else's avatar", async () => {
  await env.withSecurityRulesDisabled((ctx) => uploadBytes(ref(ctx.storage(), `profile_photos/${ALICE}.jpg`), jpeg));
  await assertFails(deleteObject(ref(storage(EVE), `profile_photos/${ALICE}.jpg`)));
});
