# Scan recommendations

## Architecture

`DiseaseRepository` loads the four unchanged classifier labels from JSON assets
in `app/src/main/assets/diseases/` (English) or `diseases_tl/` (Tagalog).
`DiseaseInfo` contains scientific name, symptoms, treatment and prevention.
Each `DiseaseTip` has an `id`, `essential`, `title` and `description`.

`ScanProcessor` consumes the classifier's result and saves the scan through
`ScanHistoryStore`. `RecommendationProvider` creates a bilingual recommendation
snapshot for the scan. `ScannerActivity.selectTab` displays the appropriate
category from that snapshot in the existing cards. Library requests continue to
use full lists. The separate Disease Library screen's static descriptive XML is
unchanged. No classifier, label order, UI layout or Supabase changes are involved.

## Current recommendation content

The Treatment and Prevention pools use all 58 recommendations supplied in the
October 21 update, with a corresponding natural Tagalog translation for each.
English descriptions preserve the supplied wording verbatim. Symptoms remain
separate and unchanged; treatment/prevention content was not moved into them.

| Class label | Treatment | Prevention |
| --- | ---: | ---: |
| Healthy | 8 | 7 |
| Black Sigatoka | 7 | 6 |
| Panama Disease | 8 | 8 |
| Cordana Leaf Spot | 7 | 7 |

Counts apply to each language: 58 English entries and 58 matching Tagalog entries.
The supplied content is the source of this update; no independent agronomic
validation is claimed and no products, dosages, intervals or cures were invented.
Conditional wording is preserved, including recommended-only Panama plant removal
and the emerging/complementary, not established, status of Cordana biological control.

## Selection and essential advice

`RecommendationSelector` keeps session-only queues per disease and category.
Optional IDs are shuffled once, selected from the front and moved to the back.
This favors advice not recently shown. All essential tips are retained. Normally
three tips are displayed per category, leaving optional content for later scans.
Every updated Treatment/Prevention pool can vary. Tips remain in authored order;
variation changes the selected membership instead of merely reordering text.
Repeated IDs are invalid and identical title/description entries are deduplicated.

Essential Treatment tips:

- Healthy: preserve healthy foliage (item 8).
- Black Sigatoka: avoid fungicide-only reliance and integrate approaches (2, 7).
- Panama: prevention/containment and the generic-fungicide caution (1, 7).
- Cordana: manage drainage, moisture and plant stress (4).

Essential Prevention tips:

- Healthy: avoid unnecessary removal of healthy leaves (3).
- Black Sigatoka: responsible fungicide/resistance management (6).
- Panama: prevent contaminated soil and planting-material introduction (2, 3).
- Cordana: water, drainage and plant health (5).

These flags prioritize the supplied advice; optional does not mean unimportant.
All pools stay within the detected disease and category. Selection happens once
on canonical English IDs; localization maps the same IDs to Tagalog, rather than
making a second independent selection.

## Language and historical compatibility

The existing `LocaleHelper` preference (`settings/language`) controls the localized
Activity context. `DiseaseRepository` and `RecommendationProvider` use its resource
locale: English by default, Tagalog for `tl`/`fil`. No new language preference or
selection system was introduced. Recognized technical terms remain where helpful.

New content uses versioned IDs such as `panama.treatment.v2.3`; previous IDs were
not reassigned to different advice. `RecommendationProvider` stores complete
English and Tagalog snapshots in local SharedPreferences, keyed by scan ID.
Changing tabs, language, Activity or process does not reroll a saved result.
Snapshots from before this content update retain their old IDs and wording, even
if those IDs no longer appear in current assets.

Old history without a snapshot follows the existing full-list fallback: it uses
the current complete lists on first opening and then saves them. This can produce
a longer section than a new scan. Unviewed old advice cannot be reconstructed
because those records never stored recommendation text. No snapshot migration or
database/Supabase schema change is required.

Snapshot cleanup retains every scan still in SQLite history (up to 200 records),
plus a current result if its history save failed. Snapshots are device-local;
clearing app data removes them. Newly added advice affects new snapshots, not
existing historical snapshots.

## Adding advice

Edit only the appropriate disease's JSON files in both language folders. Add the
same unique stable ID and essential flag to both translations, for example:

```json
{"id": "black_sigatoka.prevention.v2.7", "essential": false,
 "title": "Reviewed title", "description": "Agriculturally reviewed advice"}
```

Use a genuinely distinct recommendation and keep it in the correct category.
Never reuse an ID for a different topic. Preserve cautions in both translations.
Add a new versioned ID when replacing a topic. No Kotlin changes are needed.
Obtain agricultural review for future advice; disease labels alone do not reveal
severity, weather, local suitability or field conditions.

## Verification

`RecommendationSelectorTest` checks actual assets, all 16 disease/category/language
combinations, expected pool counts, membership variation, category/disease isolation,
essential retention, unique tips, language-ID parity, conditional cautions and
small/all-essential pool behavior. `RecommendationHistoryTest` checks Android
snapshot recreation, all four bilingual selections, legacy full-list fallback
and preservation of snapshots containing pre-update IDs and wording.

Manual checks: repeated scans, switching diseases and tabs, switching languages,
reopening new and old history, a full force-stop/relaunch, and reading the longer
legacy sections on a small screen. Automated content checks verify structure and
translation pairing; native-speaker and agricultural review of wording remains useful.
