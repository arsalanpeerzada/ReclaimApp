# Reclaim: gamification design

Arsalan Peerzada · personal project · version 2, 15 October 2026

Reclaim counts the Instagram and TikTok reels a person watches and uses white-hat gamification to help them watch fewer. Counting is a commodity; the behaviour design on top of it is the product.

## Purpose and design principle

Don't build an addictive app to fight addictive apps.

- **Their playbook:** Instagram and TikTok run on black-hat drives: variable rewards, fear of missing out, loss anxiety.
- **Our playbook:** white-hat drives: mastery, meaning, autonomy.
- **North-star metric:** less time in *all* apps, Reclaim included. Not daily active users.

## Theory stack

Four models, each with one job.

| Model | Role | What it means in practice |
|---|---|---|
| Self-Determination Theory | Foundation | Autonomy (the user sets the budget), competence (visible progress), relatedness (squads that cheer each other on, never shame) |
| Octalysis (Yu-kai Chou) | Drive selection | Lead with accomplishment, empowerment and meaning. Use black-hat drives (loss avoidance, scarcity) sparingly and with a safety net |
| Fogg Behaviour Model (B = MAP) | Intervention timing | Add friction at the moment of temptation; remove friction from the good behaviour |
| Hook model, reversed | Loop design | The trigger becomes a pause; the reward comes from stopping, not continuing |

## Core loop

Set budget → scroll (every reel counted) → mindful pause at 80% → reward (the garden grows) → weekly reflection (adjust the budget) → next week.

The user sets the budget, the app counts and nudges, staying under budget grows their world, and the weekly reflection resets the next cycle.

## Mascot: Pip

Pip is a yellow blob with an antenna who lives in the user's garden. Pip's look follows today's budget:

| Share of budget | Pip |
|---|---|
| Under 80% | Bright, smiling |
| 80 to 100% | Wary, flat mouth |
| Just over | Worried brows, a sweat drop |
| 125%+ | Frowning, losing colour |
| 150%+ | Heavy eyes, tears, antenna drooping |
| 250% | Grey and heartbroken |

This is the one deliberate use of loss avoidance, and it has guard rails: Pip shows only his own feelings, the copy never blames the user, and Pip is fully recovered the next morning.

## Gamification mechanics

| Mechanic | Drive | Why it works |
|---|---|---|
| Daily reel budget, user-set | Autonomy | Ownership beats imposed limits |
| Reclaimed time (e.g. 300 reels skipped is about 25 minutes, one book chapter) | Meaning | Turns an abstract number into a story |
| Pip's garden: a plant for every day within budget, a blossom tree every 7th; the newest plant droops after a day over and perks up after a good one | Accomplishment | Visual progress that's never taken away |
| Streaks with freezes (start with one, earn one every 7 good days, hold two) | Loss avoidance (controlled) | Consistency without the "I broke it, why bother" collapse |
| Mindful pause at 80% and 100% of the budget ("Still choosing this?" plus one breath) | Fogg friction | Interrupts autopilot at the exact moment |
| Live counter pill over Instagram and TikTok, with a meter that keeps filling past the budget | Feedback | Awareness while it matters, not afterwards |
| Offline quests, three a day (walk 10 minutes, read 10 pages) | Empowerment | Replaces the habit instead of only removing it |
| Squad cards shared with 3 to 5 friends | Relatedness | Co-op accountability beats leaderboards, which demotivate most of the field |
| Surprise rare blooms for quests only | Variable reward, inverted | Uses the slot-machine mechanic for the user |
| Identity levels: Scroller, Intentional (7 good days), Master (30) | Competence and identity | People keep behaviours that match who they think they are; a bad day never costs a level |

## Anti-patterns

Things Reclaim will not ship, because each undermines the goal.

- **Shame notifications** ("You wasted 3 hours!"): guilt drives relapse, not change.
- **Competitive "least scrolling" leaderboards:** invite cheating and resentment.
- **Pay-to-unlock extra scroll time:** monetises the problem we're solving.
- **Hard blocks by default:** people uninstall. Offer them only as opt-in commitment devices.

## Success metrics

Behaviour change and wellbeing, not engagement.

| Metric | What we track | Cadence | In the app |
|---|---|---|---|
| Primary | Trend in reels per day; % of days within budget | 30-day rolling | Weekly reflection |
| Wellbeing | WHO-5 Well-Being Index check-in | Monthly | Check-in screen |
| Insight | Mood tag against reel count, shown back to the user (e.g. "You scroll 2x more on low-mood days") | Weekly | Weekly reflection, Insights |
| Graduation | People who no longer need the app, counted as a win, not churn | When a steady month is reached | Graduation card on Today |

Graduation: 30 tracked days, at least 90% within budget, and a daily average at most half the user's usual day.

## Platform approach

Android ships first because it's the only platform that allows true reel counting.

| Platform | Counting method | Constraint |
|---|---|---|
| Android | AccessibilityService reading scroll positions in Instagram and TikTok | Google Play scrutinises AccessibilityService use; needs an in-app disclosure with consent and a declaration form |
| iOS | Screen Time API (FamilyControls / DeviceActivity) | Time and usage thresholds only; per-reel counts aren't possible because of app sandboxing |
| Web | Browser extension counting reel URL changes | Prototype only |

### What counting looks like on a real device (Pixel 8, 14 October 2026)

- **Instagram** reports reel positions in its reel viewer (`clips_viewer_view_pager`). Counting is by position: accurate.
- **TikTok** reports no positions. Counting is by swipe bursts, ignoring TikTok's follow-up scroll about a second after a swipe.

## Phasing

| Phase | Scope | Status |
|---|---|---|
| 1 | Counter, daily budget, streaks, reclaimed time | Built |
| 2 | Garden, mindful pause, offline quests | Built |
| 3 | Squads (share cards), weekly reflection, WHO-5, mood tags | Built |
| Next | Admin impact dashboard, motion polish, on-device counting accuracy | In progress |
| iOS (parallel) | Time-based version with the same gamification layer | Not started |

## Privacy and permissions

Trust is the product's licence to operate.

- **On-device by default:** counts are stored as daily totals on the phone; no content is ever read or kept. Nothing leaves the phone unless the user opts in.
- **Opt-in impact stats:** with a yes, one anonymous summary per finished day goes to Firebase, so the developer can show aggregate impact (for example to investors) on an admin dashboard. Mood and WHO-5 scores need a second, separate yes. Stopping deletes everything shared.
- **Transparent permissions:** before the user switches counting on, the app says exactly what the service reads, ignores, shows and keeps, and asks them to agree.
- **User control:** live counter and pause can be switched off; "Delete all my data" wipes everything.

A data protection and security review is needed before any public release. Mood and WHO-5 scores may count as health data under UK and EU GDPR.

## Decisions made

| Question | Decision |
|---|---|
| Whose project? | Personal |
| Growing-world theme | Garden, with Pip as its resident |
| Visual direction | Multicolour, gamified: cream ground, violet, sunflower, coral, sky, mint, pink; Bricolage Grotesque and DM Sans |
| Squads | Serverless share cards: no accounts or friend lists needed |
| Developer insights | Opt-in anonymous daily summaries to Firebase, read by an admin-only dashboard with a clearly marked sample-data mode |
| Nudge threshold | 80% of the daily budget |

## Open questions

- Monetisation that doesn't conflict with the north-star metric.
- Metric cadences and thresholds (graduation, levels, freezes) are first guesses, to validate with real use.
- Reclaimed-time conversion assumes about 5 seconds per reel; to validate with real usage data.
- App name and application id before a Play release.
