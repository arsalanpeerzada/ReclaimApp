# Reclaim

An Android app that counts the Instagram and TikTok reels you watch and uses white-hat gamification to help you watch fewer. It's built from the design in [`docs/DESIGN.md`](docs/DESIGN.md). Its north star is less time in every app, Reclaim included.

Personal project. Working name and application id (`io.github.arsalanpeerzada.reclaim`) to be settled before any Play release.

## What it does

| Area | Features |
|---|---|
| Counting | Accessibility service that counts reels in Instagram and TikTok on the phone; live counter pill at the top of those apps with a meter that keeps going past the budget |
| Phase 1 | Daily budget you choose, streaks with freezes, reclaimed time, identity levels |
| Phase 2 | Pip's garden (grows on good days, droops gently on bad ones), mindful pause at 80% and 100%, offline quests with surprise blooms |
| Phase 3 | Daily mood tag, weekly reflection with a budget suggestion, monthly WHO-5 check-in, squad share card (no server) |
| Outcomes | 30-day trend, graduation when Reclaim is no longer needed |
| Impact | Opt-in anonymous daily summaries to Firebase, and an admin dashboard for investor-ready impact numbers |
| Privacy | Nothing leaves the phone without an opt-in; separate consent for mood and WHO-5; stop-and-delete; "Delete all my data" |

Pip, the mascot, gets sadder the further you go over budget. He only ever shows his own feelings: the copy never blames the user, and he's fine again the next morning.

## Building

Needs Android Studio with JDK 17 or later and Android SDK 37.

```bash
./gradlew installDebug
./gradlew testDebugUnitTest
```

Release builds are signed from `keystore.properties` (copy `keystore.properties.example`; Git ignores the real file). Without it they're unsigned.

## Admin dashboard and Firebase

The app can share anonymous daily summaries (opt-in only; see `PRIVACY.md`) to a Firebase project, and `dashboard/` shows the impact across everyone: reel reduction, days within budget, mindful pause stop rate, retention, WHO-5 change and graduations, plus a copyable pitch summary.

Until there are real users, the dashboard runs on **sample data**, marked with a banner on every view. Never present sample numbers as results.

To connect your own Firebase project:

1. Create a Firebase project. Add an Android app with id `io.github.arsalanpeerzada.reclaim` and put its `google-services.json` in `app/` (Git ignores it). Without it the app builds normally and simply doesn't offer sharing.
2. Enable **Anonymous** and **Google** sign-in in Firebase Authentication, and create a Firestore database in your chosen region.
3. In the Firebase console, add a Firestore document `config/admins` with a field `emails` (array) holding your admin Google account(s). Then deploy the rules and indexes: `npx firebase-tools deploy --only firestore`. Admin emails live only in that document, never in this repo.
4. Add a web app in Firebase, copy `dashboard/firebase-config.example.js` to `dashboard/firebase-config.js` and paste its config.
5. Serve the dashboard (`python -m http.server 5173 --directory dashboard`, or `firebase deploy --only hosting`), choose **Live data** and sign in with the admin account.

## How counting works

`counting/ReelDetector.kt` decides, per scroll event, whether a new reel is on screen, using the best evidence each app gives:

- **Instagram** reports reel positions in its `clips_viewer_view_pager` (confirmed on a Pixel 8, 14 October 2026). It counts once when a swipe lands.
- **TikTok** (`com.zhiliaoapp.musically:id/viewpager`) reported no positions in that test. It counts by swipe bursts, ignoring TikTok's follow-up scroll about a second later.

To check accuracy on a phone, open Settings › Counting diagnostics. On a debug build connected over adb, run `adb logcat -s ReclaimCounter`. Both show structure only (positions, sizes, view ids), never content.

## Project layout

```
app/src/main/java/io/github/arsalanpeerzada/reclaim/
  counting/   accessibility service, reel detector, overlays (pill, mindful pause)
  data/       Room database (v3) and DataStore settings
  domain/     pure rules: streaks, levels, garden, quests, pause, reflections, WHO-5, trends
  ui/         Compose screens and the design system (theme, Pip, components)
```

The domain rules are pure Kotlin with unit tests in `app/src/test`.

## Before a public release

- [ ] Check counting accuracy on devices, especially TikTok
- [ ] Data protection and security review, as the design doc requires (see `PRIVACY.md`), including consent wording, Firestore region and retention
- [ ] Google Play Data safety form, listing the opt-in shared data
- [ ] Google Play accessibility declaration (draft in `docs/play-accessibility-declaration.md`)
- [ ] Confirm WHO-5 attribution wording on the official WHO-5 page
- [ ] Settle the app name and application id
- [ ] Decide on a monetisation model that doesn't conflict with the north-star metric (open question in the design doc)

Fonts: Bricolage Grotesque and DM Sans, both under the SIL Open Font License (see `third_party/fonts`).
