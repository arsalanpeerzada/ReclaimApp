# Monetisation and tuning (proposal)

Proposal, 16 October 2026, for the open questions in `DESIGN.md`. The constraint is the north-star metric: **less time in all apps, Reclaim included.** Anything that pays more when people scroll more, or open Reclaim more, is out.

## Ruled out

| Model | Why not |
|---|---|
| Ads | Paid by attention, the thing we're trying to give back |
| Pay to unlock extra scroll time | Monetises the problem (design doc anti-pattern) |
| Paywalled core counting or the pause | The behaviour change is the product; locking it away hurts the people it's for |
| Selling data | Breaks the privacy promise and likely the law for health data |

## Options that fit

| Model | How it works | Fit with the north star | Notes |
|---|---|---|---|
| **One-time "Supporter" unlock** (recommended first) | A single payment unlocks cosmetic extras: garden themes and rare plants, Pip outfits, extra share-card designs, a home-screen widget | Strong: pays once, so there's no incentive to keep people engaged | Simple, honest, matches a game-like app. Core features stay free |
| Pay what you want / tip jar | Optional in-app tip after graduation or a good month | Strong: tied to success, not usage | Low revenue; a good complement |
| Graduation pricing | A small payment when someone graduates, like a course certificate and a printable garden poster | Strong: money arrives when the app has done its job | Novel story for investors |
| B2B wellbeing (schools, employers, clinics) | Organisations pay per seat for aggregate, anonymous group dashboards; individuals use it free | Good, if group data stays aggregate and opt-in | Biggest revenue potential; needs privacy and consent design for groups |
| Annual subscription | Advanced insights (hour-of-day patterns, long-term trends) | Weaker: recurring payments reward retention | Possible later; price yearly, not monthly, and keep graduation celebrated |

**Recommendation:** launch free with a one-time Supporter unlock and an optional tip. Explore B2B pilots once the impact dashboard has real numbers. That gives investors a revenue story that doesn't fight the mission.

## Tuning the first guesses

These numbers are first guesses in the code. Each should be checked against real, opt-in data once there are enough users (suggested: 50+ people with 4+ weeks each).

| Setting | Now | What to look at in the dashboard | Where in code |
|---|---|---|---|
| Seconds per reel (reclaimed time) | 5 | Compare with screen-time per reel from a small user study | `domain/ReclaimedTime.kt` |
| Nudge at | 80% of budget | Pause stop rate at 80% vs 100% | `domain/Progress.kt`, `domain/MindfulPause.kt` |
| Freezes | Start 1, +1 per 7 good days, max 2 | How often streaks survive thanks to a freeze vs break | `domain/Streaks.kt` |
| Levels | Intentional at 7, Master at 30 good days | Share of users reaching each level by week 4 | `domain/Levels.kt` |
| Graduation | 30 days, 90% within budget, at most half the usual day | How many reach it, and whether graduates come back | `domain/Trends.kt` |
| Budget suggestion | ±10% after a week | Whether people accept it, and days within budget the week after | `domain/Wellbeing.kt` |
| Commitment lock | +5 reels after a 60 s wait, once a day | How often the unlock is used; uninstalls after enabling | `domain/CommitmentLock.kt` |

A good next step is moving these into Firebase Remote Config, so they can be tuned without an app update.
