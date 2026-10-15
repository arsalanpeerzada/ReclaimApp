# Reclaim privacy notice

**Draft, 15 October 2026. Not reviewed.** Have a qualified data protection reviewer check it before any public release. Reclaim stores wellbeing data, which is likely special category (health) data under UK GDPR and EU GDPR, and can share some of it when the user opts in.

## The short version

Everything Reclaim knows stays on your phone unless you choose to share anonymous stats. Sharing is off until you say yes, mood and wellbeing scores need a second, separate yes, and you can stop and delete what you've shared at any time. There are no ads and no backups.

## What the accessibility service reads

| From | What | Why |
|---|---|---|
| Every app | Only the name of the app that comes to the front | So the live counter shows in Instagram and TikTok and hides everywhere else |
| Instagram and TikTok only | Scroll positions, the size of the scrolling area and its view id | To tell when a new full-screen reel is showing |

It never reads text, messages, captions, images, video or anything you type, in any app. Scroll events from other apps are dropped before anything about them is read.

## What Reclaim stores on your phone

| Data | Detail kept | Purpose |
|---|---|---|
| Reel counts | One total per app per day | Budget, streaks, garden, trends |
| Daily budget | The budget that applied each day | So changing it doesn't rewrite history |
| Mindful pauses | How many showed and how many ended the session, per day | Insights, and the impact stats if shared |
| Settings | Budget, usual day, start date, switches, reflection, graduation and sharing choices | Running the app |
| Quests | Which quests you ticked off, by day | Garden flowers |
| Mood tags | One number (1 to 5) per day you tag | The weekly mood insight |
| WHO-5 check-ins | The total score only, by day; individual answers aren't kept | Showing your wellbeing trend back to you |

**Not stored:** individual reels, times you watched, what you watched, or anything about other apps.

The counting diagnostics screen keeps the last 80 counting decisions in memory only. Debug builds also write them to the Android log.

## Anonymous stats (opt-in)

If you say yes in onboarding or Settings › Anonymous stats, Reclaim uploads one summary per finished day to the developer's Firebase project (Google Cloud Firestore), under an anonymous Firebase account. It has no name, email or device id.

| Shared | Only with the second yes | Never shared |
|---|---|---|
| Daily reel totals for Instagram and TikTok, budget, whether you stayed within it, quests ticked off, mindful pause outcomes, days since you started, level, streak, graduation date, app version | Daily mood tags, WHO-5 scores | What you watch, other apps, times of day, your name, email, contacts, location, device id |

- **Purpose:** to show, in aggregate, whether Reclaim helps people scroll less and feel better. The developer sees totals across everyone through an admin dashboard and may share those aggregate results, for example with investors.
- **Lawful basis (to confirm in review):** consent. For mood and WHO-5 data, explicit consent through the separate switch.
- **Where:** Firebase (Google). The region depends on the project's Firestore location, which should be chosen before launch (an EU region is the simplest fit for EU users).
- **Retention (to decide):** suggested 24 months from upload, then deletion.
- **Stopping:** switching sharing off deletes everything this phone shared, along with its anonymous account. "Delete all my data" does the same.

## Sharing a squad card

Nothing is sent unless you tap **Share my week**. That creates an image showing counts only and hands it to the app you choose.

## Your control

- Switch counting off at any time in Android's accessibility settings.
- Switch off the live counter and the mindful pause in Settings.
- Stop anonymous stats and delete what was shared in Settings › Anonymous stats.
- Delete everything in Settings › Your data › Delete all my data.
- Uninstalling deletes everything on the phone. App backup is switched off (`allowBackup="false"`). If you shared stats, stop sharing before uninstalling, as an uninstalled app can't delete its upload.

## Open points for the reviewer

- Consent wording, explicit consent for health data, and whether a DPIA is needed.
- Firestore region, retention period and a way to request deletion after uninstalling.
- Google Play's prominent disclosure and consent requirements (see `docs/play-accessibility-declaration.md`), and the Data safety form, which must now list the shared data.
- Whether reading the front app's name from every app needs more prominence.
- WHO-5 terms of use and attribution, to confirm on the official WHO-5 page.
