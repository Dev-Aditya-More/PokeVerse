const { planWeeklyRewards, weekKey } = require("./weeklyRewards");

// ── Weekly placement coins ───────────────────────────────────────────────────
// Everyone who played the week gets coins, scaled by where they finished (rules and amounts live in
// weeklyRewards.js, which is unit-tested). Paid straight into users/{uid}.progression.earned with an
// increment, which is how the app's wallet already merges ("earned only ever goes up"), and recorded
// as users/{uid}.weeklyReward so the app can tell the player what they won.
//
// Idempotent: a player whose weeklyReward.week already equals this week's key is skipped, so a retry
// or a double run can never pay twice. Players without a users doc are skipped (nothing to credit).
async function payWeeklyRewards(db, FieldValue, docs, now) {
  const key = weekKey(now);
  const plan = planWeeklyRewards(docs.map((d) => ({ uid: d.id, weeklyXp: d.data().weeklyXp })));
  const byUid = new Map();
  let paid = 0;
  let totalCoins = 0;

  const chunkSize = 200;
  for (let i = 0; i < plan.length; i += chunkSize) {
    const chunk = plan.slice(i, i + chunkSize);
    const refs = chunk.map((p) => db.collection("users").doc(p.uid));
    const snaps = await db.getAll(...refs);
    const batch = db.batch();
    chunk.forEach((p, idx) => {
      const snap = snaps[idx];
      if (!snap.exists) return;
      if (snap.data().weeklyReward && snap.data().weeklyReward.week === key) return;
      batch.set(
        refs[idx],
        {
          progression: { earned: FieldValue.increment(p.coins) },
          weeklyReward: { week: key, weekOf: now, rank: p.rank, coins: p.coins, xp: p.weeklyXp },
        },
        { merge: true }
      );
      byUid.set(p.uid, p);
      paid++;
      totalCoins += p.coins;
    });
    await batch.commit();
  }
  console.log(`🪙 Weekly coins: paid ${paid} of ${plan.length} eligible trainers, ${totalCoins} coins total.`);
  return { byUid, paid, totalCoins };
}

module.exports = { payWeeklyRewards };
