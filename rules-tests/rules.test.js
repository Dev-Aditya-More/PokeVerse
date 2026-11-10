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
  Timestamp, runTransaction, serverTimestamp,
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
// ── Pokédex / Dex Daily cloud copy ───────────────────────────────────────────
test("app: pokedex + dex daily sync (merge set)", async () => {
  await seed(`users/${ALICE}`, userDoc(ALICE));
  await assertSucceeds(setDoc(doc(db(ALICE), "users", ALICE), {
    dexSeen: [1, 4, 25], dexCaught: [25], dexDailyRecords: "2026-11-09||1|0|0",
    lastDailyFactXpDate: "2026-11-09", lastDexDailyXpDate: "2026-11-09", feedbackThanksClaimed: true,
  }, { merge: true }));
});
test("app: evolution wallet + evolved forms sync", async () => {
  await seed(`users/${ALICE}`, userDoc(ALICE));
  await assertSucceeds(setDoc(doc(db(ALICE), "users", ALICE),
    { dexEvolved: [26, 2], dexEvoEarned: 120, dexEvoSpent: 25 }, { merge: true }));
});
test("attack: spend more evo shards than were earned, or mint a million", async () => {
  await seed(`users/${ALICE}`, userDoc(ALICE));
  await assertFails(setDoc(doc(db(ALICE), "users", ALICE), { dexEvoEarned: 10, dexEvoSpent: 50 }, { merge: true }));
  await assertFails(setDoc(doc(db(ALICE), "users", ALICE), { dexEvoEarned: 5000000, dexEvoSpent: 0 }, { merge: true }));
  await assertFails(setDoc(doc(db(ALICE), "users", ALICE), { dexEvolved: Array.from({ length: 5000 }, (_, i) => i) }, { merge: true }));
});
test("app: evolved-away forms sync (dexConsumed), and items sync in the wallet", async () => {
  await seed(`users/${ALICE}`, userDoc(ALICE));
  await assertSucceeds(setDoc(doc(db(ALICE), "users", ALICE), { dexConsumed: [133, 25] }, { merge: true }));
  await assertSucceeds(setDoc(doc(db(ALICE), "users", ALICE),
    { progression: { earned: 700, spent: 250, itemsBought: { "water-stone": 2, ball_great: 5 }, itemsUsed: { "water-stone": 1 } } },
    { merge: true }));
});
test("app: the bag level syncs in the wallet; attack: an absurd or mistyped level", async () => {
  await seed(`users/${ALICE}`, userDoc(ALICE));
  await assertSucceeds(setDoc(doc(db(ALICE), "users", ALICE), { progression: { earned: 900, spent: 250, bagLevel: 2 } }, { merge: true }));
  await assertFails(setDoc(doc(db(ALICE), "users", ALICE), { progression: { earned: 900, spent: 250, bagLevel: 99 } }, { merge: true }));
  await assertFails(setDoc(doc(db(ALICE), "users", ALICE), { progression: { earned: 900, spent: 250, bagLevel: "max" } }, { merge: true }));
});
test("attack: stuff dexConsumed or the item maps to use the doc as storage", async () => {
  await seed(`users/${ALICE}`, userDoc(ALICE));
  await assertFails(setDoc(doc(db(ALICE), "users", ALICE), { dexConsumed: Array.from({ length: 5000 }, (_, i) => i) }, { merge: true }));
  const many = Object.fromEntries(Array.from({ length: 200 }, (_, i) => [`k${i}`, 1]));
  await assertFails(setDoc(doc(db(ALICE), "users", ALICE),
    { progression: { earned: 700, spent: 250, itemsBought: many } }, { merge: true }));
});
test("attack: stuff the doc with a huge dex list / history string", async () => {
  await seed(`users/${ALICE}`, userDoc(ALICE));
  await assertFails(setDoc(doc(db(ALICE), "users", ALICE), { dexSeen: Array.from({ length: 5000 }, (_, i) => i) }, { merge: true }));
  await assertFails(setDoc(doc(db(ALICE), "users", ALICE), { dexDailyRecords: "x".repeat(40000) }, { merge: true }));
});
// ── Poké Coins wallet ────────────────────────────────────────────────────────
const wallet = (over = {}) => ({
  earned: 700, spent: 250, owned: ["frame_pokeball", "frame_flame"], frame: "frame_flame", title: "",
  backdrop: "", widget: "", freezesBought: 1, freezesUsed: 0, claimedLevel: 24, ...over,
});
test("app: wallet sync (merge set of the progression map)", async () => {
  await seed(`users/${ALICE}`, userDoc(ALICE));
  await assertSucceeds(setDoc(doc(db(ALICE), "users", ALICE), { progression: wallet() }, { merge: true }));
});
test("app: equipped frame/title go public on the leaderboard doc", async () => {
  await seed(`leaderboard/${ALICE}`, leaderboardDoc(ALICE));
  await assertSucceeds(setDoc(doc(db(ALICE), "leaderboard", ALICE), { frame: "frame_flame", title: "title_ace" }, { merge: true }));
});
test("app: backdrop, partner and favorite type go public on the leaderboard doc", async () => {
  await seed(`leaderboard/${ALICE}`, leaderboardDoc(ALICE));
  await assertSucceeds(setDoc(doc(db(ALICE), "leaderboard", ALICE),
    { backdrop: "backdrop_dusk", starter: "pikachu", favoriteType: "electric" }, { merge: true }));
});
test("attack: oversized backdrop / partner / type on the public doc", async () => {
  await seed(`leaderboard/${ALICE}`, leaderboardDoc(ALICE));
  await assertFails(setDoc(doc(db(ALICE), "leaderboard", ALICE), { starter: "x".repeat(40) }, { merge: true }));
  await assertFails(setDoc(doc(db(ALICE), "leaderboard", ALICE), { favoriteType: "x".repeat(40) }, { merge: true }));
});
test("attack: spend more than you ever earned", async () => {
  await seed(`users/${ALICE}`, userDoc(ALICE));
  await assertFails(setDoc(doc(db(ALICE), "users", ALICE), { progression: wallet({ spent: 800 }) }, { merge: true }));
});
test("attack: mint a million coins in one write", async () => {
  await seed(`users/${ALICE}`, userDoc(ALICE, { progression: wallet() }));
  await assertFails(setDoc(doc(db(ALICE), "users", ALICE), { progression: wallet({ earned: 900000 }) }, { merge: true }));
});
test("attack: jump the wallet far past the per-write step", async () => {
  await seed(`users/${ALICE}`, userDoc(ALICE, { progression: wallet() }));
  await assertFails(setDoc(doc(db(ALICE), "users", ALICE), { progression: wallet({ earned: 20000 }) }, { merge: true }));
});
test("attack: roll the wallet back / un-spend coins (stale device)", async () => {
  await seed(`users/${ALICE}`, userDoc(ALICE, { progression: wallet() }));
  await assertFails(setDoc(doc(db(ALICE), "users", ALICE), { progression: wallet({ spent: 0 }) }, { merge: true }));
  await assertFails(setDoc(doc(db(ALICE), "users", ALICE), { progression: wallet({ earned: 100, spent: 50 }) }, { merge: true }));
});
test("attack: more freezes used than ever bought, oversized cosmetic ids", async () => {
  await seed(`users/${ALICE}`, userDoc(ALICE));
  await assertFails(setDoc(doc(db(ALICE), "users", ALICE), { progression: wallet({ freezesUsed: 5 }) }, { merge: true }));
  await assertFails(setDoc(doc(db(ALICE), "leaderboard", ALICE), leaderboardDoc(ALICE, { title: "x".repeat(40) })));
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

// ── Online Team Battle (team_battles) ────────────────────────────────────────
const ROOM = "room1";
const roomNew = (over = {}) => ({
  hostId: ALICE, guestId: "", hostName: "Ash", guestName: "", status: "waiting", roomCode: "ABC234",
  mode: "quick", seed: 123456789, heartbeatHostAt: serverTimestamp(), heartbeatGuestAt: serverTimestamp(),
  createdAt: now(), lastUpdated: now(), ...over,
});
// An active battle as stored (plain timestamps, since seeding bypasses the rules).
const roomActive = (over = {}) => ({
  hostId: ALICE, guestId: BOB, hostName: "Ash", guestName: "Misty", status: "active", roomCode: "ABC234",
  mode: "quick", seed: 123456789, heartbeatHostAt: now(), heartbeatGuestAt: now(), createdAt: now(), lastUpdated: now(), ...over,
});
const minutesAgo = (m) => Timestamp.fromMillis(Date.now() - m * 60 * 1000);
const room = (uid) => doc(db(uid), "team_battles", ROOM);
const action = (uid, id) => doc(db(uid), "team_battles", ROOM, "actions", id);
const act = (seat, uid, step, kind = "attack", index = 0) => ({ step, seat, uid, kind, index });

test("app: host opens a room, guest joins, both bring a team and ready up, then trade actions", async () => {
  await assertSucceeds(setDoc(room(ALICE), roomNew()));
  await assertSucceeds(setDoc(doc(db(ALICE), "team_battles", ROOM, "teams", ALICE), { ids: [25, 6, 9] }));
  await assertSucceeds(updateDoc(room(BOB), {
    guestId: BOB, guestName: "Misty", status: "active", heartbeatGuestAt: serverTimestamp(), lastUpdated: now(),
  }));
  await assertSucceeds(setDoc(doc(db(BOB), "team_battles", ROOM, "teams", BOB), { ids: [7, 4] }));
  await assertSucceeds(setDoc(action(ALICE, "s0_h"), act("h", ALICE, 0, "ready")));
  await assertSucceeds(setDoc(action(BOB, "s0_g"), act("g", BOB, 0, "ready")));
  await assertSucceeds(setDoc(action(ALICE, "s1_h"), act("h", ALICE, 1, "special")));
  await assertSucceeds(setDoc(action(BOB, "s1_g"), act("g", BOB, 1, "switch", 2)));
  await assertSucceeds(getDoc(action(ALICE, "s1_g")));
});
test("app: a waiting room is discoverable by a quick-match query", async () => {
  await seed("team_battles/" + ROOM, roomActive({ status: "waiting", guestId: "", guestName: "" }));
  const open = query(collection(db(BOB), "team_battles"), where("status", "==", "waiting"), where("mode", "==", "quick"), where("guestId", "==", ""), limit(10));
  await assertSucceeds(getDocs(open));
});
test("attack: create a room as someone else, with a taken seat, or with extra fields", async () => {
  await assertFails(setDoc(room(EVE), roomNew()));
  await assertFails(setDoc(room(ALICE), roomNew({ guestId: BOB, status: "active" })));
  await assertFails(setDoc(room(ALICE), roomNew({ mode: "ranked" })));
  await assertFails(setDoc(room(ALICE), roomNew({ winner: "h" })));
  await assertFails(setDoc(room(ALICE), roomNew({ heartbeatHostAt: Timestamp.fromMillis(1) })));
});
test("attack: take a seat that is taken, join your own room, or change more than the seat", async () => {
  await seed("team_battles/" + ROOM, roomActive());
  await assertFails(updateDoc(room(EVE), { guestId: EVE, guestName: "Eve", status: "active", heartbeatGuestAt: serverTimestamp(), lastUpdated: now() }));
  await seed("team_battles/room2", roomNew({ heartbeatHostAt: now(), heartbeatGuestAt: now() }));
  await assertFails(updateDoc(doc(db(ALICE), "team_battles", "room2"), { guestId: ALICE, guestName: "Ash", status: "active", heartbeatGuestAt: serverTimestamp(), lastUpdated: now() }));
  await assertFails(updateDoc(doc(db(EVE), "team_battles", "room2"), { guestId: EVE, guestName: "Eve", status: "active", seed: 1, heartbeatGuestAt: serverTimestamp(), lastUpdated: now() }));
});
test("attack: an outsider cannot read a started battle, its teams or its actions", async () => {
  await seed("team_battles/" + ROOM, roomActive());
  await seed("team_battles/" + ROOM + "/teams/" + ALICE, { ids: [25] });
  await seed("team_battles/" + ROOM + "/actions/s0_h", act("h", ALICE, 0, "ready"));
  await assertFails(getDoc(room(EVE)));
  await assertFails(getDoc(doc(db(EVE), "team_battles", ROOM, "teams", ALICE)));
  await assertFails(getDoc(action(EVE, "s0_h")));
  await assertSucceeds(getDoc(action(BOB, "s0_h")));
});
test("attack: write the other seat's action, rewrite a recorded choice, or break the id format", async () => {
  await seed("team_battles/" + ROOM, roomActive());
  await assertFails(setDoc(action(BOB, "s1_h"), act("h", BOB, 1)));            // Bob writing the host's seat
  await assertFails(setDoc(action(BOB, "s1_g"), act("h", BOB, 1)));            // wrong seat field
  await assertFails(setDoc(action(BOB, "s1_g"), act("g", ALICE, 1)));          // someone else's uid
  await assertSucceeds(setDoc(action(BOB, "s1_g"), act("g", BOB, 1, "guard")));
  await assertFails(setDoc(action(BOB, "s1_g"), act("g", BOB, 1, "special"))); // choices are final
  // The clock's own move carries auto: true (a missed turn); anything else extra or mistyped is refused.
  await assertSucceeds(setDoc(action(BOB, "s3_g"), { ...act("g", BOB, 3, "attack"), auto: true }));
  await assertSucceeds(setDoc(action(ALICE, "s3_h"), { ...act("h", ALICE, 3, "attack"), auto: false }));
  await assertFails(setDoc(action(BOB, "s4_g"), { ...act("g", BOB, 4, "attack"), auto: "yes" }));
  await assertFails(setDoc(action(BOB, "s4_g"), { ...act("g", BOB, 4, "attack"), bonus: 9 }));
  await assertFails(setDoc(action(BOB, "move-1"), act("g", BOB, 1)));          // not s<step>_<seat>
  await assertFails(setDoc(action(BOB, "s2_g"), act("g", BOB, 2, "hack")));
  await assertFails(setDoc(action(BOB, "s2_g"), act("g", BOB, 2, "switch", 9)));
  await assertFails(setDoc(action(EVE, "s2_g"), act("g", EVE, 2)));
});
test("attack: act in a room that has not started, or write a team that is not yours / too big / twice", async () => {
  await seed("team_battles/" + ROOM, roomNew({ heartbeatHostAt: now(), heartbeatGuestAt: now() }));
  await assertFails(setDoc(action(ALICE, "s1_h"), act("h", ALICE, 1)));
  await seed("team_battles/room3", roomActive());
  await assertFails(setDoc(doc(db(BOB), "team_battles", "room3", "teams", ALICE), { ids: [1] }));
  await assertFails(setDoc(doc(db(ALICE), "team_battles", "room3", "teams", ALICE), { ids: [1, 2, 3, 4, 5, 6, 7] }));
  await assertSucceeds(setDoc(doc(db(ALICE), "team_battles", "room3", "teams", ALICE), { ids: [1, 2] }));
  await assertFails(setDoc(doc(db(ALICE), "team_battles", "room3", "teams", ALICE), { ids: [150] }));
});
test("app: heartbeats and quitting; attack: someone else's heartbeat, a future heartbeat, quitting for the rival", async () => {
  await seed("team_battles/" + ROOM, roomActive());
  await assertSucceeds(updateDoc(room(ALICE), { heartbeatHostAt: serverTimestamp() }));
  await assertSucceeds(updateDoc(room(BOB), { heartbeatGuestAt: serverTimestamp() }));
  await assertFails(updateDoc(room(BOB), { heartbeatHostAt: serverTimestamp() }));
  await assertFails(updateDoc(room(EVE), { heartbeatGuestAt: serverTimestamp() }));
  await assertFails(updateDoc(room(ALICE), { heartbeatHostAt: Timestamp.fromMillis(Date.now() + 3600000) }));
  await assertFails(updateDoc(room(BOB), { status: "finished", quitSeat: "h", lastUpdated: now() }));
  await assertSucceeds(updateDoc(room(BOB), { status: "finished", quitSeat: "g", lastUpdated: now() }));
});
test("app: close a finished or cancelled room; attack: an outsider closing it, or setting a winner", async () => {
  await seed("team_battles/" + ROOM, roomActive());
  await assertFails(updateDoc(room(EVE), { status: "finished", lastUpdated: now() }));
  await assertFails(updateDoc(room(ALICE), { status: "finished", winner: "h", lastUpdated: now() }));
  await assertSucceeds(updateDoc(room(ALICE), { status: "finished", lastUpdated: now() }));
});
test("attack: claim a forfeit against a rival who is still around; app: claim it once they are really gone", async () => {
  await seed("team_battles/" + ROOM, roomActive({ heartbeatGuestAt: now() }));
  await assertFails(updateDoc(room(ALICE), { status: "finished", abandonedSeat: "g", lastUpdated: now() }));
  await seed("team_battles/room4", roomActive({ heartbeatGuestAt: minutesAgo(5) }));
  const gone = doc(db(ALICE), "team_battles", "room4");
  // Nobody can claim against their own seat.
  await assertFails(updateDoc(gone, { status: "finished", abandonedSeat: "h", lastUpdated: now() }));
  await assertSucceeds(updateDoc(gone, { status: "finished", abandonedSeat: "g", lastUpdated: now() }));
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
