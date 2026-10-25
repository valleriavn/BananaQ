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


## Fixed prevention practices (October 25, 2026)

The latest request places the new general practices under Prevention only, as
fixed final practices for every valid classification, including Healthy. They
are not part of the randomized pools. `diseases/default_prevention.json` is a
local English asset paired with `diseases_tl/default_prevention.json`. The latest
request adds Filipino translations with identical stable IDs and essential flags.
`DefaultPrevention` composes it after the selected or historical disease advice;
`ScannerActivity` uses the existing numbered recommendation renderer. The latest
revision removes the Prevention display cap: all selected disease-specific items
remain, followed by the three fixed practices. Healthy also always includes the
existing healthy-leaf preservation advice. Duplicate topics are shown once.
The list ends after the last practice, with no source/contact footer or extra
heading; numbering continues through the complete list.
Source and contact metadata are retained in the local asset and this document.
No new screen, network dependency, database migration, prediction or model change
is involved. Treatment for Healthy is explicitly described as routine care.

Saved bilingual snapshots are not changed or rerolled. At display time the
Panama planting-material and general-biosecurity treatment items (`v2.4`, `v2.5`)
are omitted from Treatment because they belong in the permanent Prevention
practices. The matching generic Panama prevention entries (`v2.1`, `v2.4`,
`v2.8`) are omitted from the disease-specific portion to avoid duplicate cards.
All underlying snapshots and original assets remain intact. The fixed section
is deliberately current even when opening an older saved result.

### Sources and limits

The supplied link is an RCPC III office profile, not a banana recommendation
sheet: https://rfo3.da.gov.ph/index.php/regional-crop-protection-center/ . It
confirms the requested office's identity and role but does not verify the exact
harvesting-tool disinfection method, planting-supplier registration terminology,
or the incomplete farmer-related practice. The asset records that
RCPC attribution remains unverified; the interface does not claim RCPC approval of the old
user-supplied disease advice or of these summaries.

The general biosecurity, tool hygiene and disease-free planting-material summary
is corroborated by the DA-hosted Banana Production Manual, Fusarium wilt
management section:
https://hvcdp.da.gov.ph/wp-content/uploads/2022/05/Banana-Production-Manual.pdf .
These are prevention precautions, not treatments for Black Sigatoka, Cordana or
Panama Disease. No chemical, dilution, exposure time, dose or schedule was added.
Supplier registration/accreditation is a question for the local agriculture
office, not an asserted official requirement. The exact RCPC original remains
needed. The uncertain farmer-own-equipment practice is omitted until its meaning
and official source are confirmed.

Sto. Tomas, Batangas is in Region IV-A (CALABARZON), not RCPC III's Region III:
https://calabarzon.denr.gov.ph/regional-profile/ . The app distinguishes RCPC III
from local assistance instead of calling it the Sto. Tomas office.

### Contact audit

No Sto. Tomas agriculture phone number was present in tracked app source/assets
or documentation at implementation time. The existing development-team phone
`09917583912` in the user agreement was left unchanged and was not relabeled as
a DA number. Added the city government's verified City Agriculture Office listing:
`(043) 784-8022 local 307`, `agri@cityofstotomas.gov.ph`, Basement Floor, City Hall
Main Building, Gov. Malvar St., Poblacion I; Monday-Friday 8 AM-5 PM.
Source: https://cityofstotomas.gov.ph/city-agriculture-office/ (checked October 25,
2026). The city office is labeled as local agricultural assistance, not an RCPC
III branch. Contact and source information remain stored offline in the asset. The latest
request removes their display after practice five; it does not delete the
retained telephone number or overwrite the development-team contact.

`DefaultPreventionTest` exercises the real result tabs for all four classifications
in English and Filipino at compact width and enlarged font, verifies fixed order,
uncapped lists, presence of all fixed practices, absence of footer content, tab reopening, scrolling and no
placement in Treatment.
It also checks rotation-independent composition and snapshot stability. The
existing recommendation selector, screenshot, responsive and other JVM tests
remain enabled.


### Scan-result spacing revision

Summary and expanded results share the page horizontal inset. The detail sheet
uses a 4dp top inset, 16dp bottom inset, 8dp gap before confidence progress,
16dp section/tab gaps and 8dp between the section title and recommendations.
The 48dp drag-handle and tab touch targets remain unchanged. Numbered items use
12dp between the badge and text, 4dp between title and description, 2dp extra
line spacing, and 8dp on each side of a separating divider. Dividers appear only
between items, so there is no extra separator or doubled gap after the final item.
The same renderer handles Symptoms, Treatment and Prevention.


### Bilingual cleanup and rendering ownership

The fixed practices and Healthy routine-care note now follow the current app
language (`en`, `tl` or `fil`). Existing saved bilingual disease snapshots keep
all original IDs and wording; the default practices remain a separate current
appendix without a five-item display limit.

`ui/scan/ScanRecommendationRenderer` owns the shared numbered item views and tab
content. `ScannerActivity` continues to own camera, lifecycle, animations and
navigation. Both languages use the same layout and spacing code. Obsolete
additional-section heading data was removed because that heading is no longer
shown. Source and city contact metadata remain in both offline JSON assets.

`DiseaseRepository` caches successfully parsed assets by their localized path,
so repeated calls do not reopen and parse the same bundled file. The cache lives
with its repository, not in a global Activity reference. `RecommendationProvider`
accepts the repository for reuse and caches only its most recent bilingual
snapshot; it cannot accumulate every scanned result in memory. Persistent keys,
cleanup rules, full-list historical fallback and recommendation rotation are
unchanged.


### Fixed prevention across every scan

There is no five-item cap. New scans continue to rotate their existing optional
disease-specific advice, with all three fixed farm precautions appended every
time. Full-list historical results keep all nonduplicate entries. Numbering is
continuous, and there is no additional heading or footer. Healthy results also
always show the existing `healthy.prevention.v2.3` advice: preserve healthy leaves
and avoid unnecessary leaf removal. It is copied into the bilingual defaults
from the existing Healthy assets, not a new treatment or chemical prescription.
The matching pool item is omitted from the variable portion to avoid duplication.
Saved recommendation snapshots and their rotation rules are unchanged.


### Account agricultural assistance

Account > Help & Guidance now includes Agricultural Assistance (Tulong sa
Agrikultura). Its scrollable dialog reuses the localized office name, contact,
hours, address and official directory URL already stored in the default-practice
assets. Both assets store a structured `office` object with separate name, phone,
extension, dialNumber, email, address and hours fields. `office.dialNumber` is
`+63437848022`, so a callable number is never guessed from translated display text.
The visible contact retains local extension 307. Call office uses ACTION_DIAL
for the base number; it never places a call automatically or adds CALL_PHONE
permission. The dialog remains available when returning from the dialer, so the
user can read the extension. The official directory opens only on a user tap.
No network is needed to read or copy the contact details. The dialog follows the
existing window sizing and scrolling helpers, rejects duplicate opens, and is
dismissed when its Activity is destroyed. Scan-result recommendations remain
unchanged and do not display a contact footer.


### Readability and larger controls

The Account entry is now Farming help / Tulong sa pagsasaka. Account information
rows use 18sp text and a 64dp minimum touch height. The contact dialog uses a
wider usable window (up to 480dp, 96% of the available width), 24dp content
padding and one vertical reading column. The dialog heading shares the 22sp Account page style and the
office introduction is 16sp. The phone number is 24sp, extension 14sp, detail
labels and values 14sp, and buttons use 16sp text with a 48dp minimum height.
Groups are separated by 16dp, with 4dp between each detail label and its value.
The primary call action sits immediately under the phone and extension, before
secondary details. A dark green call button with a phone icon has stronger
contrast than the earlier lighter green. Office hours, address and email are
separate labeled groups. The close action says Back to Account. Values and
buttons can wrap at enlarged font sizes; nothing scales the user's font setting
down. All content still scrolls on short windows, with the same behavior in both
languages. The removed contact-check note has not been restored.

Office data is stored in `office` rather than packed into one presentation
paragraph. A computed `contact` property retains the readable combined summary
for existing data checks, with no duplicate mutable number or extension.
