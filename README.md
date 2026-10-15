# Reclaim

An Android app that counts the Instagram and TikTok reels you watch and uses white-hat gamification to help you watch fewer. It's built from the design in `Digital Wellbeing App - Gamification Design.pdf`. Its north star is less time in every app, Reclaim included.

Personal project. Working name and application id (`io.github.arsalanpeerzada.reclaim`) to be settled before any Play release.

## What it does

| Area | Features |
|---|---|
| Counting | Accessibility service that counts reels in Instagram and TikTok on the phone; live counter pill at the top of those apps with a meter that keeps going past the budget |
| Phase 1 | Daily budget you choose, streaks with freezes, reclaimed time, identity levels |
| Phase 2 | Pip's garden (grows on good days, droops gently on bad ones), mindful pause at 80% and 100%, offline quests with surprise blooms |
| Phase 3 | Daily mood tag, weekly reflection with a budget suggestion, monthly WHO-5 check-in, squad share card (no server) |
| Outcomes | 30-day trend, graduation when Reclaim is no longer needed |
| Privacy | No internet permission, no accounts, no backups, "Delete all my data" in Settings |

Pip, the mascot, gets sadder the further you go over budget. He only ever shows his own feelings: the copy never blames the user, and he's fine again the next morning.

## Building

Needs Android Studio with JDK 17 or later and Android SDK 37.

```bash
./gradlew installDebug
./gradlew testDebugUnitTest
```

Release builds are signed from `keystore.properties` (copy `keystore.properties.example`; Git ignores the real file). Without it they're unsigned.

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
- [ ] Data protection and security review, as the design doc requires (see `PRIVACY.md`)
- [ ] Google Play accessibility declaration (draft in `docs/play-accessibility-declaration.md`)
- [ ] Confirm WHO-5 attribution wording on the official WHO-5 page
- [ ] Settle the app name and application id
- [ ] Decide on a monetisation model that doesn't conflict with the north-star metric (open question in the design doc)

Fonts: Bricolage Grotesque and DM Sans, both under the SIL Open Font License (see `third_party/fonts`).
