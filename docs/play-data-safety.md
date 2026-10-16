# Google Play Data safety form (draft answers)

**Draft, 16 October 2026.** Check each answer against the live form in Play Console and the current [Data safety guidance](https://support.google.com/googleplay/android-developer/answer/10787469) before submitting. Answers reflect the app as built (version 0.9.0).

## Overview

| Question | Answer | Why |
|---|---|---|
| Does your app collect or share any of the required user data types? | **Yes** | Opt-in anonymous daily summaries are sent to Firebase |
| Is all user data encrypted in transit? | **Yes** | Firestore uses TLS |
| Do you provide a way for users to request that their data is deleted? | **Yes** | Settings › Anonymous stats (stop and delete) and Settings › Your data |

## Data types

| Data type (Play category) | Collected | Shared with third parties | Optional | Purpose |
|---|---|---|---|---|
| App activity › Other user-generated content / App interactions: daily reel totals, budgets, quests, pause outcomes | Yes, only if the user opts in | No (Firebase is a service provider) | Yes | Analytics (aggregate impact) |
| Health and fitness › Health info: mood tags, WHO-5 scores | Yes, only with the second opt-in | No | Yes | Analytics (aggregate impact) |
| Device or other IDs | **Check:** Firebase anonymous auth creates an app-instance id. Decide with the reviewer whether to declare it | No | Yes | Keeping one phone's summaries together |

Not collected: location, personal info (name, email), financial info, messages, photos or video, audio, files, calendar, contacts, web browsing, app info and performance (no crash reporting or analytics SDK).

**On-device only, not "collected":** per Play's definitions, data processed only on the device isn't collected. The accessibility service's data never leaves the phone.

## Before submitting

- [ ] Confirm Firebase's own data practices are declared correctly for Authentication and Firestore (Google publishes guidance for its SDKs).
- [ ] Privacy policy URL: publish `PRIVACY.md` as a web page and link it in the listing and in the app.
- [ ] Keep these answers in sync with `PRIVACY.md` and the accessibility declaration.
