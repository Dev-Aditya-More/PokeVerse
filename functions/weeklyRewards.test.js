const test = require("node:test");
const assert = require("node:assert/strict");
const { COINS, MIN_ACTIVE_XP, coinsForRank, planWeeklyRewards, weekKey } = require("./weeklyRewards");

const players = (xps) => xps.map((weeklyXp, i) => ({ uid: `u${i}`, weeklyXp }));
const coinsOf = (plan, uid) => plan.find((p) => p.uid === uid)?.coins;

test("tiers: crown, podium, top 10, everyone else", () => {
  assert.equal(coinsForRank(1, 900), 500);
  assert.equal(coinsForRank(2, 800), 300);
  assert.equal(coinsForRank(3, 700), 200);
  assert.equal(coinsForRank(4, 600), 100);
  assert.equal(coinsForRank(10, 300), 100);
  assert.equal(coinsForRank(11, 300), 50);
  assert.equal(coinsForRank(500, 60), 50);
});

test("nobody is paid for a week they barely played", () => {
  assert.equal(coinsForRank(1, 0), 0);
  assert.equal(coinsForRank(1, MIN_ACTIVE_XP - 1), 0);
  assert.equal(coinsForRank(40, MIN_ACTIVE_XP), 50);
});

test("a 15-player week pays the documented amounts", () => {
  const xp = [1000, 900, 800, 700, 650, 600, 550, 500, 450, 400, 300, 200, 100, 60, 10];
  const plan = planWeeklyRewards(players(xp));
  assert.equal(coinsOf(plan, "u0"), 500);
  assert.equal(coinsOf(plan, "u1"), 300);
  assert.equal(coinsOf(plan, "u2"), 200);
  for (let i = 3; i <= 9; i++) assert.equal(coinsOf(plan, `u${i}`), 100, `rank ${i + 1}`);
  for (let i = 10; i <= 13; i++) assert.equal(coinsOf(plan, `u${i}`), 50, `rank ${i + 1}`);
  assert.equal(coinsOf(plan, "u14"), undefined, "10 XP is below the activity bar");
});

test("order of the input does not matter", () => {
  const a = planWeeklyRewards(players([300, 900, 100, 700]));
  const b = planWeeklyRewards(players([700, 100, 900, 300]).map((p, i) => ({ ...p, uid: ["u3", "u2", "u1", "u0"][i] })));
  assert.equal(coinsOf(a, "u1"), 500);
  assert.equal(coinsOf(b, "u1"), 500);
});

test("a tie shares the rank and the full prize", () => {
  const plan = planWeeklyRewards(players([800, 800, 500]));
  assert.equal(coinsOf(plan, "u0"), 500);
  assert.equal(coinsOf(plan, "u1"), 500);
  // Two players share rank 1, so the next distinct score is rank 3, not rank 2.
  assert.equal(plan.find((p) => p.uid === "u2").rank, 3);
  assert.equal(coinsOf(plan, "u2"), 200);
});

test("bad rows are ignored instead of paying out or throwing", () => {
  const plan = planWeeklyRewards([{ uid: "", weeklyXp: 999 }, { uid: "ok", weeklyXp: "abc" }, { uid: "fine", weeklyXp: 120 }]);
  assert.deepEqual(plan.map((p) => p.uid), ["fine"]);
});

test("the most one player can ever be paid in a week", () => {
  const plan = planWeeklyRewards(players([99999]));
  assert.ok(plan.every((p) => p.coins <= COINS.first));
});

test("week key is the IST date, so a run at Monday 00:00 IST keys to that Monday", () => {
  // Monday 2026-11-16 00:00 IST == Sunday 2026-11-15 18:30 UTC
  assert.equal(weekKey(Date.UTC(2026, 10, 15, 18, 30)), "2026-11-16");
  assert.equal(weekKey(Date.UTC(2026, 10, 15, 18, 29)), "2026-11-15");
});
