# Reclaim privacy notice

**Draft, 15 October 2026. Not reviewed.** Have a qualified data protection reviewer check it before any public release. The design doc requires a Data Protection and Security review, and Reclaim stores wellbeing data, which is likely special category (health) data under UK GDPR and EU GDPR.

## The short version

Reclaim keeps everything on your phone. It has no internet permission, so it cannot send anything anywhere. It has no accounts, no analytics, no ads and no backups. You can delete everything from Settings › Your data.

## What the accessibility service reads

| From | What | Why |
|---|---|---|
| Every app | Only the name of the app that comes to the front | So the live counter shows in Instagram and TikTok and hides everywhere else |
| Instagram and TikTok only | Scroll positions, the size of the scrolling area and its view id | To tell when a new full-screen reel is showing |

It never reads text, messages, captions, images, video or anything you type, in any app. Scroll events from other apps are dropped before anything about them is read.

## What Reclaim stores, on your phone only

| Data | Detail kept | Purpose |
|---|---|---|
| Reel counts | One total per app per day | Budget, streaks, garden, trends |
| Daily budget | The budget that applied each day | So changing it doesn't rewrite history |
| Settings | Budget, usual day, start date, switches, reflection and graduation dates | Running the app |
| Quests | Which quests you ticked off, by day | Garden flowers |
| Mood tags | One number (1 to 5) per day you tag | The weekly mood insight |
| WHO-5 check-ins | The total score only, by day; individual answers aren't kept | Showing your wellbeing trend back to you |

**Not stored:** individual reels, times you watched, what you watched, or anything about other apps.

The counting diagnostics screen keeps the last 80 counting decisions in memory only, and they're gone when the app closes. Debug builds also write those decisions to the Android log.

## Sharing

Nothing is shared unless you tap **Share my week**. That creates an image showing counts only (days within budget, streak, reels a day, minutes won back) and hands it to the app you choose. What happens to it after that is up to you and that app.

## Your control

- Switch counting off at any time in Android's accessibility settings.
- Switch off the live counter and the mindful pause in Settings.
- Delete everything in Settings › Your data › Delete all my data. There is no other copy to delete.
- Uninstalling Reclaim also deletes all its data. App backup is switched off (`allowBackup="false"`).

## Open points for the reviewer

- Lawful basis and explicit consent for mood and WHO-5 data, if these count as health data.
- Whether the in-app disclosure meets Google Play's prominent disclosure and consent requirements (see `docs/play-accessibility-declaration.md`).
- Whether reading the front app's name from every app needs to be called out more prominently.
- The WHO-5 terms of use and attribution, which need confirming on the official WHO-5 page.
