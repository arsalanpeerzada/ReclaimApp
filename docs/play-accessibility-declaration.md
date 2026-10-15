# Google Play: AccessibilityService declaration (draft)

**Draft, 15 October 2026.** Check every point against the current [Use of the AccessibilityService API](https://support.google.com/googleplay/android-developer/answer/10964491) policy page before submitting. The summary below is from that page's search snippets and mirrors, not a full read of the current text.

## Policy points that apply

- Reclaim is **not** an accessibility tool (it isn't designed to help people with disabilities), so it must not set `isAccessibilityTool="true"`. It doesn't set it, so it defaults to false.
- Non-tool apps need an **in-app prominent disclosure** of the data accessed and how it's used, with an **affirmative consent action**. The disclosure can't live only in the store listing, privacy policy or terms.
- Non-tool apps complete the **Permission Declaration Form** in Play Console.

## How Reclaim meets them

| Requirement | Where in the app |
|---|---|
| Prominent disclosure | The last onboarding step and Settings: "It reads / It ignores / It can show / It keeps" cards (`PermissionCards` in `ui/components/Components.kt`) |
| Affirmative consent | "Agree and open settings" button, shown only below the disclosure. Every path to Android's accessibility settings goes through it, including Today's "Switch on" |
| Service description | `accessibility_service_description` in `res/values/strings.xml`, shown by Android on the service's settings page |
| Data stays on device by default | The service's data is stored on the phone. The `INTERNET` permission is used only for opt-in anonymous daily totals (Settings › Anonymous stats), which never include anything the service reads from other apps |

## Draft answers for the declaration form

**Core functionality that needs the API**
Reclaim counts the short videos (reels) a user watches in Instagram and TikTok, so they can see and limit their own use against a daily budget they set. Android offers no other API that reveals when a new full-screen reel is shown inside another app. Usage stats only give time in an app, not the number of reels.

**Data accessed and how it's used**
For every app, the service reads only the package name of the app that comes to the front, to show or hide Reclaim's on-screen counter. For Instagram and TikTok only, it reads scroll positions, the size of the scrolling container and its view id, to detect a new reel. It reads no text, images, video or user input. Daily totals are stored on the device. Only if the user separately opts in are anonymous daily totals (counts, never content) uploaded for aggregate impact statistics.

**User-facing features that depend on it**
Live reel counter over Instagram and TikTok, daily budget and streaks, mindful pause screen at 80% and 100% of the budget, weekly reflection.

**Video demonstration**
To record: onboarding disclosure, the consent button, enabling the service, the counter appearing in Instagram, the pause screen, and the Today screen updating.
