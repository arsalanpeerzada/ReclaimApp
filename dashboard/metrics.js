// Impact metrics for the Reclaim admin dashboard. Pure functions: the same code runs on live
// Firestore data and on the clearly labelled sample data.
//
// Input shape (what the app uploads, see app/.../stats/StatsSummaries.kt):
//   users:    [{ uid, startDay, typicalReelsPerDay, dailyBudget, level, streak, graduatedOn, sharesWellbeing }]
//   days:     [{ uid, date, dayIndex, reels, budget, within, instagram, tiktok, quests, pausesShown, pausesStopped, mood? }]
//   checkins: [{ uid, date, score }]
// Dates are "YYYY-MM-DD" strings.

export const SECONDS_PER_REEL = 5;

const DAY_MS = 86_400_000;
const toDate = (s) => new Date(s + "T00:00:00Z");
export const addDays = (s, n) => new Date(toDate(s).getTime() + n * DAY_MS).toISOString().slice(0, 10);
const daysBetween = (a, b) => Math.round((toDate(b) - toDate(a)) / DAY_MS);
const mean = (xs) => (xs.length ? xs.reduce((a, b) => a + b, 0) / xs.length : null);
const median = (xs) => {
  if (!xs.length) return null;
  const s = [...xs].sort((a, b) => a - b);
  const m = Math.floor(s.length / 2);
  return s.length % 2 ? s[m] : (s[m - 1] + s[m]) / 2;
};
const pct = (part, whole) => (whole ? (part / whole) * 100 : null);
const groupBy = (xs, key) => xs.reduce((m, x) => (m.get(x[key])?.push(x) ?? m.set(x[key], [x]), m), new Map());

/** Every headline number and chart series the dashboard shows. */
export function computeMetrics({ users, days, checkins }, today) {
  const daysByUser = groupBy(days, "uid");
  for (const list of daysByUser.values()) list.sort((a, b) => (a.date < b.date ? -1 : 1));
  const userById = new Map(users.map((u) => [u.uid, u]));

  // People
  const active7 = new Set(days.filter((d) => daysBetween(d.date, today) <= 7).map((d) => d.uid)).size;
  const new30 = users.filter((u) => u.startDay && daysBetween(u.startDay, today) <= 30).length;

  // Behaviour change: first week against the most recent week, for people with two weeks or more.
  const changes = [];
  const vsUsual = [];
  for (const [uid, list] of daysByUser) {
    if (list.length < 14) continue;
    const first = mean(list.slice(0, 7).map((d) => d.reels));
    const last = mean(list.slice(-7).map((d) => d.reels));
    if (first > 0) changes.push(((last - first) / first) * 100);
    const usual = userById.get(uid)?.typicalReelsPerDay;
    if (usual > 0) vsUsual.push(((last - usual) / usual) * 100);
  }

  const recent = days.filter((d) => daysBetween(d.date, today) <= 30);
  const shown = days.reduce((a, d) => a + (d.pausesShown || 0), 0);
  const stopped = days.reduce((a, d) => a + (d.pausesStopped || 0), 0);

  // Retention: of people who started at least N days ago, how many still share a day at N or later.
  const retention = (n) => {
    const eligible = users.filter((u) => u.startDay && daysBetween(u.startDay, today) >= n + 1);
    const kept = eligible.filter((u) => (daysByUser.get(u.uid) || []).some((d) => d.dayIndex >= n));
    return { rate: pct(kept.length, eligible.length), eligible: eligible.length };
  };

  // Wellbeing: first against latest WHO-5, for people with two check-ins or more.
  const who5Pairs = [];
  for (const list of groupBy(checkins, "uid").values()) {
    if (list.length < 2) continue;
    list.sort((a, b) => (a.date < b.date ? -1 : 1));
    who5Pairs.push([list[0].score, list[list.length - 1].score]);
  }

  // Mood: reels on low-mood days (1-2) against the rest.
  const moodDays = days.filter((d) => d.mood != null);
  const low = mean(moodDays.filter((d) => d.mood <= 2).map((d) => d.reels));
  const rest = mean(moodDays.filter((d) => d.mood > 2).map((d) => d.reels));

  const eligibleToGraduate = users.filter((u) => u.startDay && daysBetween(u.startDay, today) >= 30);
  const minutesReclaimed = days.reduce((sum, d) => {
    const usual = userById.get(d.uid)?.typicalReelsPerDay || 0;
    return sum + (Math.max(0, usual - d.reels) * SECONDS_PER_REEL) / 60;
  }, 0);

  return {
    users: users.length,
    active7,
    new30,
    daysShared: days.length,
    medianChangePercent: median(changes),
    cutByQuarterPercent: pct(changes.filter((c) => c <= -25).length, changes.length),
    usersWithTwoWeeks: changes.length,
    medianVsUsualPercent: median(vsUsual),
    withinBudgetPercent: pct(recent.filter((d) => d.within).length, recent.length),
    pauseStopPercent: pct(stopped, shown),
    pausesShown: shown,
    retentionD7: retention(7),
    retentionD30: retention(30),
    who5: {
      people: who5Pairs.length,
      first: mean(who5Pairs.map((p) => p[0])),
      latest: mean(who5Pairs.map((p) => p[1])),
      aboveCutoffFirst: pct(who5Pairs.filter((p) => p[0] > 50).length, who5Pairs.length),
      aboveCutoffLatest: pct(who5Pairs.filter((p) => p[1] > 50).length, who5Pairs.length),
    },
    moodRatio: low != null && rest > 0 ? low / rest : null,
    moodDays: moodDays.length,
    graduates: users.filter((u) => u.graduatedOn).length,
    graduationPercent: pct(eligibleToGraduate.filter((u) => u.graduatedOn).length, eligibleToGraduate.length),
    hoursReclaimed: minutesReclaimed / 60,
    questsDone: days.reduce((a, d) => a + (d.quests || 0), 0),
    appSplit: {
      instagram: days.reduce((a, d) => a + (d.instagram || 0), 0),
      tiktok: days.reduce((a, d) => a + (d.tiktok || 0), 0),
    },
    cohortCurve: cohortCurve(days),
    weeklyActive: weeklyActive(days, today),
  };
}

/** Average reels per day by days since starting, where at least five people contribute. */
function cohortCurve(days, maxDay = 60, minPeople = 5) {
  const byIndex = groupBy(days.filter((d) => d.dayIndex <= maxDay), "dayIndex");
  return [...byIndex.entries()]
    .filter(([, list]) => list.length >= minPeople)
    .map(([dayIndex, list]) => ({ x: dayIndex, y: mean(list.map((d) => d.reels)), people: list.length }))
    .sort((a, b) => a.x - b.x);
}

/** People sharing at least one day in each of the last twelve weeks. */
function weeklyActive(days, today, weeks = 12) {
  const out = [];
  for (let w = weeks - 1; w >= 0; w--) {
    const end = addDays(today, -7 * w);
    const start = addDays(end, -6);
    const people = new Set(days.filter((d) => d.date >= start && d.date <= end).map((d) => d.uid)).size;
    out.push({ x: start, y: people });
  }
  return out;
}

const fmt = (n, digits = 0) => (n == null ? "n/a" : n.toLocaleString("en-GB", { maximumFractionDigits: digits }));

/** Plain-text bullets for a pitch deck, with the caveats that keep them honest. */
export function pitchSummary(m, { sample }) {
  const lines = [
    sample ? "ILLUSTRATIVE SAMPLE DATA, NOT REAL USERS. Do not present these as results." : null,
    `${fmt(m.users)} people sharing anonymous stats; ${fmt(m.active7)} active in the last 7 days.`,
    m.medianChangePercent != null
      ? `Median change in reels per day, first week to latest week: ${fmt(m.medianChangePercent)}% (${fmt(m.usersWithTwoWeeks)} people with 2+ weeks).`
      : null,
    m.cutByQuarterPercent != null ? `${fmt(m.cutByQuarterPercent)}% of them cut their reels by a quarter or more.` : null,
    m.withinBudgetPercent != null ? `${fmt(m.withinBudgetPercent)}% of days in the last 30 were within the person's own budget.` : null,
    m.pauseStopPercent != null ? `The mindful pause ended the session ${fmt(m.pauseStopPercent)}% of the times it appeared (${fmt(m.pausesShown)} pauses).` : null,
    m.who5.people ? `WHO-5 wellbeing: average ${fmt(m.who5.first)} at the first check-in, ${fmt(m.who5.latest)} at the latest (${fmt(m.who5.people)} people).` : null,
    m.retentionD30.rate != null ? `30-day retention ${fmt(m.retentionD30.rate)}% (graduates leave on purpose and count as a success).` : null,
    `${fmt(m.hoursReclaimed)} hours won back against people's own usual day; ${fmt(m.questsDone)} offline quests done.`,
    "Opt-in, anonymous data from people who chose to share; not a controlled study.",
  ];
  return lines.filter(Boolean).map((l) => `- ${l}`).join("\n");
}
