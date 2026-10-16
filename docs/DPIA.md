# Data protection impact assessment (draft)

**Draft, 16 October 2026. Not legal advice.** Prepared to help a qualified data protection reviewer; every conclusion below needs their sign-off before public release. Laws to check: UK GDPR, EU GDPR (if EU users are accepted) and Pakistan's data protection law as in force at release.

## 1. Why an assessment

Reclaim processes data about people's phone habits and, with consent, their mood and WHO-5 wellbeing scores. Mood and wellbeing scores are likely **health data** (special category). A DPIA is the safe default for health data and for monitoring behaviour, even at small scale.

## 2. What is processed

| Data | Where | Who can see it | Lawful basis (proposed) |
|---|---|---|---|
| Daily reel totals per app, budgets, quests, pause outcomes, mood tags, WHO-5 totals | On the phone (Room, DataStore) | The user only | Not shared, so not processed by the developer |
| Daily summaries: reel totals, budget, within budget, quests, pause outcomes, day index, level, streak, graduation date, app version | Firestore, `me-central1` (Doha), only after opt-in | The developer, via the admin dashboard | Consent (Art. 6(1)(a)) |
| Mood tags and WHO-5 scores in those summaries | Same, only after a second, separate opt-in | Same | Explicit consent (Art. 9(2)(a)) |
| Anonymous Firebase account id | Firebase Authentication | Firebase project owner | Necessary to keep each phone's summaries apart |

**Never processed:** what people watch, other apps' content, text, images, typing, times of day, names, emails, contacts, location, device ids.

The accessibility service reads, for every app, only the name of the app in front, and, for Instagram and TikTok only, scroll positions and the scroller's size and id. This stays on the phone.

## 3. Necessity and proportionality

- **Minimised:** daily totals only; no per-reel records or timestamps.
- **Pseudonymous:** anonymous account id; no identity is collected. Anonymous ≠ anonymised: the reviewer should treat the data as personal data.
- **Opt-in, granular, withdrawable:** sharing is off by default; health data has its own switch; stopping deletes everything shared and the anonymous account.
- **Purpose:** aggregate impact statistics (behaviour change, wellbeing) to show whether the app works, including to investors in aggregate form. No individual-level data is shown outside the admin dashboard.

## 4. Risks and mitigations

| Risk | Likelihood / impact | Mitigation in place | Still to decide |
|---|---|---|---|
| Re-identification from a small user base | Medium / medium | No identity fields; aggregates only in pitches | Minimum group size (for example, never report a figure based on fewer than 10 people) |
| Unauthorised access to shared data | Low / high | Firestore rules: owner-only writes with field validation; admin reads gated by `config/admins` | Turn on App Check; review admin list regularly |
| Data kept longer than needed | Medium / medium | Delete on withdrawal | Retention period (proposed: 24 months from upload, then deletion) and how it's enforced |
| Uninstall without withdrawing | Medium / low | Privacy notice warns users | A deletion request route that works without the app (for example, an email address and an in-app copyable id) |
| International transfer | Depends on users' countries | Region `me-central1` (Doha) | Transfer assessment for UK/EU users; whether Pakistan law requires local processing |
| Accessibility service over-reach | Low / high | Reads app name from every app only; content never read; Google Play disclosure with consent | Play review |
| Wellbeing scores causing distress | Low / medium | No diagnosis; gentle wording; suggests talking to someone or a GP | Add local helpline links for launch countries |

## 5. Data subject rights

- **Access and portability:** the data is on the user's phone; a "copy my data" export would make this easy (not built yet).
- **Erasure:** Settings › Anonymous stats › stop (deletes shared data); Settings › Your data › Delete all my data.
- **Withdrawal of consent:** the same switches, at any time.

## 6. Sign-off

| Role | Name | Date | Decision |
|---|---|---|---|
| Data protection reviewer | | | |
| Developer | Arsalan Peerzada | | |
