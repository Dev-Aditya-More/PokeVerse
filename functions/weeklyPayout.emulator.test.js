// Runs against the Firestore emulator:
//   firebase emulators:exec --only firestore --project demo-dexverse "node --test weeklyPayout.emulator.test.js"
const test = require("node:test");
const { beforeEach } = require("node:test");
const assert = require("node:assert/strict");
const admin = require("firebase-admin");
const { payWeeklyRewards } = require("./weeklyPayout");

process.env.FIRESTORE_EMULATOR_HOST = process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8080";
admin.initializeApp({ projectId: "demo-dexverse" });
const db = admin.firestore();
const FV = admin.firestore.FieldValue;

// Each test starts from an empty database: ranks depend on everyone on the board.
beforeEach(async () => {
  await db.recursiveDelete(db.collection("leaderboard"));
  await db.recursiveDelete(db.collection("users"));
});

const NOW = Date.UTC(2026, 10, 15, 18, 30); // Monday 2026-11-16 00:00 IST

async function seed(rows, { withUserDoc = true } = {}) {
  for (const [uid, weeklyXp, earned] of rows) {
    await db.collection("leaderboard").doc(uid).set({ uid, weeklyXp });
    if (withUserDoc) await db.collection("users").doc(uid).set({ uid, progression: { earned, spent: 0 } });
  }
}
async function board() {
  return (await db.collection("leaderboard").orderBy("weeklyXp", "desc").get()).docs;
}
const user = async (uid) => (await db.collection("users").doc(uid).get()).data();

test("pays by placement into the wallet and records the result", async () => {
  await seed([["a", 900, 100], ["b", 700, 0], ["c", 100, 20], ["d", 5, 7]]);
  const res = await payWeeklyRewards(db, FV, await board(), NOW);

  assert.equal(res.paid, 3);
  assert.equal((await user("a")).progression.earned, 600); // 100 + 500 for first
  assert.equal((await user("b")).progression.earned, 300); // 0 + 300 for second
  assert.equal((await user("c")).progression.earned, 220); // 20 + 200 for third
  assert.equal((await user("d")).progression.earned, 7, "5 XP is below the activity bar");
  assert.deepEqual((await user("a")).weeklyReward, { week: "2026-11-16", weekOf: NOW, rank: 1, coins: 500, xp: 900 });
  assert.equal((await user("a")).progression.spent, 0, "other wallet fields are untouched");
});

test("running twice in the same week never pays twice", async () => {
  await seed([["x", 500, 0]]);
  await payWeeklyRewards(db, FV, await board(), NOW);
  const again = await payWeeklyRewards(db, FV, await board(), NOW + 60_000);
  assert.equal(again.paid, 0);
  assert.equal((await user("x")).progression.earned, 500);
});

test("the next week pays again", async () => {
  await seed([["y", 500, 0]]);
  await payWeeklyRewards(db, FV, await board(), NOW);
  await payWeeklyRewards(db, FV, await board(), NOW + 7 * 24 * 3600 * 1000);
  assert.equal((await user("y")).progression.earned, 1000);
});

test("a leaderboard entry with no users doc is skipped, not created", async () => {
  await seed([["ghost", 800, 0]], { withUserDoc: false });
  const res = await payWeeklyRewards(db, FV, await board(), NOW);
  assert.equal(res.paid, 0);
  assert.equal((await db.collection("users").doc("ghost").get()).exists, false);
});

test("a player with no progression map yet still gets credited", async () => {
  await db.collection("leaderboard").doc("fresh").set({ uid: "fresh", weeklyXp: 300 });
  await db.collection("users").doc("fresh").set({ uid: "fresh" });
  await payWeeklyRewards(db, FV, await board(), NOW);
  assert.equal((await user("fresh")).progression.earned, 500);
});
