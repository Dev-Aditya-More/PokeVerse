// Weekly leaderboard payouts: pure rules, no Firebase imports, so they can be unit-tested
// (node --test weeklyRewards.test.js) before anything is paid to a real player.
//
// Everyone who played properly gets something, and placement multiplies it:
//   #1 .......... 500 coins
//   #2 .......... 300
//   #3 .......... 200
//   #4 - #10 .... 100
//   everyone else who earned at least MIN_ACTIVE_XP that week .... 50
//
// For scale: a casual day of play earns ~30 coins and a frame costs 250, so the floor is worth
// about two days of play and the crown about two weeks. Coins only buy cosmetics.

const MIN_ACTIVE_XP = 50; // two daily check-ins, or one finished game: "played this week", not "opened the app once"

const COINS = Object.freeze({ first: 500, second: 300, third: 200, top10: 100, active: 50 });

/** Coins for a finishing [rank] (1-based, ties share a rank) with [weeklyXp] earned that week. */
function coinsForRank(rank, weeklyXp) {
  if (!Number.isFinite(weeklyXp) || weeklyXp < MIN_ACTIVE_XP) return 0;
  if (rank === 1) return COINS.first;
  if (rank === 2) return COINS.second;
  if (rank === 3) return COINS.third;
  if (rank <= 10) return COINS.top10;
  return COINS.active;
}

/**
 * @param {{uid: string, weeklyXp: number}[]} players leaderboard rows, in any order
 * @returns {{uid: string, rank: number, weeklyXp: number, coins: number}[]} only players owed coins
 *
 * Standard competition ranking: players with equal XP share a rank and a payout, so a tie for
 * first pays both players the full first-place prize instead of favouring whoever sorts first.
 */
function planWeeklyRewards(players) {
  const rows = players
    .map((p) => ({ uid: p.uid, weeklyXp: Number(p.weeklyXp) || 0 }))
    .filter((p) => p.uid);
  const sorted = [...rows].sort((a, b) => b.weeklyXp - a.weeklyXp);

  const out = [];
  let rank = 0;
  let previousXp = null;
  sorted.forEach((p, index) => {
    if (p.weeklyXp !== previousXp) {
      rank = index + 1;
      previousXp = p.weeklyXp;
    }
    const coins = coinsForRank(rank, p.weeklyXp);
    if (coins > 0) out.push({ uid: p.uid, rank, weeklyXp: p.weeklyXp, coins });
  });
  return out;
}

/** The IST calendar date (yyyy-MM-dd) of the run: the idempotency key for "already paid this week". */
function weekKey(nowMs) {
  return new Date(nowMs + 5.5 * 60 * 60 * 1000).toISOString().slice(0, 10);
}

// The first Monday that pays out (IST date, same format as weekKey). The update shipped on Friday
// 13 Nov 2026, so the week of 9-15 Nov was mostly played before anyone knew about coins; the first payout
// is for 16-22 Nov, the first full week with the update, paid on Monday 23 Nov.
const FIRST_PAYOUT_WEEK = "2026-11-23";

/** Whether the Monday run at [nowMs] pays coins (false before FIRST_PAYOUT_WEEK). */
function payoutsStarted(nowMs) {
  return weekKey(nowMs) >= FIRST_PAYOUT_WEEK;
}

module.exports = { COINS, MIN_ACTIVE_XP, FIRST_PAYOUT_WEEK, coinsForRank, planWeeklyRewards, weekKey, payoutsStarted };
