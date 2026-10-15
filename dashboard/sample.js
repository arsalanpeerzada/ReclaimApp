// Synthetic users for rehearsing the dashboard before real data exists. Every view built from this
// is marked SAMPLE DATA. Never present these numbers as results.

import { addDays } from "./metrics.js";

/** Small seeded random generator, so the sample is the same every time. */
function mulberry32(seed) {
  return () => {
    seed |= 0;
    seed = (seed + 0x6d2b79f5) | 0;
    let t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

export function sampleData(today, people = 240, seed = 7) {
  const rand = mulberry32(seed);
  const users = [];
  const days = [];
  const checkins = [];

  for (let i = 0; i < people; i++) {
    const uid = `sample-${i}`;
    const started = Math.floor(rand() * 90) + 1;
    const startDay = addDays(today, -started);
    const usual = 80 + Math.round(rand() * 320);
    const budget = Math.max(10, Math.round((usual * (0.4 + rand() * 0.3)) / 5) * 5);
    const responsiveness = 0.2 + rand() * 0.5;
    const staysFor = rand() < 0.35 ? Math.floor(rand() * 20) + 3 : started;
    const sharesWellbeing = rand() < 0.6;
    let within = 0;

    for (let d = 0; d < Math.min(started, staysFor); d++) {
      const trend = 1 - responsiveness * (1 - Math.exp(-d / 14));
      const reels = Math.max(0, Math.round(usual * trend * (0.75 + rand() * 0.5)));
      const isWithin = reels <= budget;
      if (isWithin) within++;
      const shown = reels >= budget * 0.8 ? 1 + (reels >= budget ? 1 : 0) : 0;
      const mood = sharesWellbeing && rand() < 0.6 ? Math.max(1, Math.min(5, Math.round(4 - (reels / usual) * 1.5 + (rand() - 0.5) * 2))) : null;
      days.push({
        uid,
        date: addDays(startDay, d),
        dayIndex: d,
        reels,
        budget,
        within: isWithin,
        instagram: Math.round(reels * (0.4 + rand() * 0.3)),
        tiktok: 0,
        quests: rand() < 0.45 ? 1 + Math.floor(rand() * 3) : 0,
        pausesShown: shown,
        pausesStopped: shown && rand() < 0.4 ? 1 : 0,
        mood,
      });
      days[days.length - 1].tiktok = reels - days[days.length - 1].instagram;
    }
    if (sharesWellbeing) {
      let score = 36 + Math.round(rand() * 30);
      for (let c = 0; c < Math.min(started, staysFor); c += 28) {
        checkins.push({ uid, date: addDays(startDay, c), score });
        score = Math.min(100, score + Math.round(rand() * 10) - 1);
      }
    }
    const graduatedOn = started > 45 && staysFor === started && within / started > 0.85 && rand() < 0.5 ? addDays(today, -Math.floor(rand() * 10)) : null;
    users.push({ uid, startDay, typicalReelsPerDay: usual, dailyBudget: budget, level: within >= 30 ? 3 : within >= 7 ? 2 : 1, streak: 0, graduatedOn, sharesWellbeing });
  }
  return { users, days, checkins };
}
