# UI spacing and alignment audit — 21 October 2026

## Scope and preserved behavior

Reviewed all 12 activity layouts, three repeated-list layouts, the shared brand header,
four custom dialogs, the scanner sheet, feedback form/saved state, navigation, styles,
qualified dimensions, responsive layout helpers, and programmatically generated rows.
The app remains Kotlin/XML Views, with the same colors, typefaces, assets, content,
navigation, classification, persistence, and sync behavior. No dependency was added.

## Problems corrected

| Area | Cause | Correction |
| --- | --- | --- |
| Screen edges | Headers used 25dp where bodies used 24dp; landscape headers used 20dp beside 24dp bodies. | Header padding aliases the same qualified page-padding resource. |
| Shared brand headers | Other pages used 24sp text, 32dp logos, larger padding and full-height language pills beside Home’s compact version. | All standard brand headers now share 20sp text, 28dp logos, 6dp top/bottom spacing and 13sp language labels. Visually inset pills preserve 48dp touch targets. |
| Page-title sizes | Account/history/feedback/manual/disease titles ranged from 23–32sp. | Shared 22sp page headings match Home’s greeting; section headings retain their hierarchy. |
| Wide page headings | Fixed titles occupied the full window while bodies were capped at 720dp. | Account/history/feedback titles use a bounded padded heading; manual title rows use the same bounded column as their body. |
| Feedback form | 20dp form edges differed from the page grid, and form/saved states started at different top offsets. | Both use qualified page edges and a 16dp title-to-content gap. |
| Rating choices | Centering content groups caused icons to shift when one label needed more lines. | Options and rows top-align their icons while labels remain centered and wrap naturally. |
| Agreement footer | 40dp body padding and a 32dp action margin accumulated into 72dp of empty space. | One 24dp content-bottom gap; action margin no longer adds a second footer. |
| Equivalent sections | Title gaps, section gaps, card insets, repeated-item gaps, and action spacers used unrelated near-equal values. | Shared 4dp-based tokens and role aliases establish consistent relationships. |
| Dialogs | Four shells used different outer padding and different paired-action gaps. | 24dp dialog padding, consistent internal spacing, and 8dp action separation; content still scrolls within the usable window. |
| Scanner recommendations | Divider indentation was a fixed 40dp even when the number badge grew with font scaling. | Divider and text share the measured badge width plus the shared item gap. |
| Compact IME form | Fixed brand and title bands can occupy the input area in short keyboard windows. | Only in short IME windows, feedback temporarily collapses those bands and restores them when space returns. The form stays scrollable and navigation remains available. |
| Home library | Content ended high above navigation; replacing it with a flexible blank spacer moved the empty band into the middle. | The illustration region takes spare height without distorting the asset, keeping the library near navigation with a 24dp outer bottom gap. Short windows keep natural scrollable content. |
| Looping cards | A finite list stopped at the last disease, while centered snapping in the earlier loop caused discontinuity. | Cached repeated cards support both swipe directions without an end stop or snap/recenter jump. |

## Spacing system

`app/src/main/res/values/spacing.xml` defines 4, 8, 12, 16, 24, 32, 40, and 48dp tokens.
The role dimensions are:

- `page_heading_gap`: 16dp between a shell and page heading or a heading and body.
- `section_gap`: 24dp between unrelated groups.
- `card_content_padding`: 16dp; compact content uses 12dp.
- `dialog_content_padding`: 24dp.
- `content_bottom_padding`: 24dp, applied once.
- `page_horizontal_padding`: 16dp on compact windows, 24dp on regular phones/landscape,
  and 32dp on wider windows; header padding resolves to the same value.

Art dimensions, touch-target bounds, strokes, corners, and typography are not spacing
scale entries. Optical geometry remains explicit: 22dp accent-card text inset is the
existing 6dp stripe plus 16dp padding; 52dp label clearance reserves a 48dp icon action
plus 4dp; a 40dp flag centered in a 52dp pill requires a 6dp edge. The previously requested
compact header uses the same 6dp vertical treatment across standard pages. Centered
onboarding artwork and camera overlay retain their role-specific layouts. Text continues
to use sp with the system font scale; dimensions use dp.

## Files modified

- `app/src/main/res/values/spacing.xml`: scale, role aliases, and documented optical clearances.
- `values/dimensions.xml`, `values-sw360dp/dimensions.xml`, `values-sw600dp/dimensions.xml`,
  `values-land/dimensions.xml`: shared qualified page/header boundaries.
- `values/manual_styles.xml`, `values/account_styles.xml`: reusable card, section, rating,
  and row spacing.
- All 12 `activity_*.xml` files under `app/src/main/res/layout`: shared spacing references;
  focused alignment and hierarchy corrections described above.
- `include_account_brand_header.xml`: consistent brand padding and gaps.
- `item_disease_library.xml`, `item_history_scan.xml`, `item_history_header.xml`: consistent
  repeated-card, badge, and section spacing.
- `dialog_avatar_picker.xml`, `dialog_avatar_preview.xml`, `dialog_feedback_confirmation.xml`,
  `dialog_unidentified_scan.xml`: consistent shell padding and internal/action gaps.
- `ContentColumn.kt`: reused for bounded manual title rows; its implementation is unchanged.
- `ResponsiveHeaderRow.kt`, `ResponsiveDateTimeRow.kt`, `ResponsiveActionRow.kt`,
  `ResponsiveScanRow.kt`, `ResponsiveTabRow.kt`: shared resource gaps in native measurement.
- `ScannerActivity.kt`: recommendation row gaps, title/body separation, and measured divider indentation.
- `AccountActivity.kt`: avatar tile padding/margins share the same grid; tile sizing reserves that padding.
- `DialogSizing.kt`: dialog breathing room uses the shared 16dp token.
- `SystemBarInsets.kt`, `FeedbackDetailActivity.kt`: compact keyboard header policy; insets still apply once.
- `MainActivity.kt`, `DiseaseLibraryAdapter.kt`: restored cached native looping and retained scroll position.
- `ResponsiveLayoutTest.kt`, `ScalingScreenshotTest.kt`, `WindowBehaviorTest.kt`: boundary,
  rating alignment, library-bottom, loop-direction, and compact-keyboard checks.
- `app/src/test/screenshots/*.png`: reviewed native-layout references for the changed spacing.

Java/Kotlin filenames above are under `app/src/main/java/bananaq`; tests
are under the equivalent `src/test` and `src/androidTest` package directories.

## Verification

Native JVM renders cover 72 screenshots across normal/enlarged phones, compact Filipino,
landscape, a tall 411×890dp phone, and a 1024×768dp wide window. Populated-page geometry
checks cover 72 combinations of six window sizes, three densities, two font scales
(100%/200%), and English/Filipino. Checks now also compare header/page grid resources,
wide account-title/body alignment, rating-icon alignment, and library bottom spacing.

The device suite includes actual keyboard open/close and submit access; dialog actions;
synthetic short-IME header restoration and repeated cutout/bar/keyboard delivery;
looping forward/backward across cycle boundaries; opening disease details; locale/layout
checks; model inference; and tap/drag scanner sheet behavior. No app data is cleared.

Device: Samsung SM-A166P, Android API 36, 1080×2340 pixels, 420dpi display override,
font scale 1.08. No emulator was used.

Final results: 42 JVM tests passed (39 app, 3 scanner domain), all 72 screenshot
comparisons passed, lintDebug and both debug APK assemblies succeeded. The updated
app and instrumentation APK were installed with `adb install -r`, preserving data.
All 15 selected device tests passed. The final phone home capture was reviewed at
`app/build/reports/scaling/device-home-final.png`; device test output is saved at
`app/build/reports/device-spacing-tests.txt`. `git diff --check` passed.

Header geometry assertions run across the full configuration matrix, checking the
shared brand text size and logo dimensions. Reviewed renders include standard phone,
compact Filipino with enlarged text, disease details, and wide feedback layouts.

## Limits and follow-up

The size/density/font matrix uses simulated native layouts, not 72 physical devices.
Hardware verification covers one phone. Actual API 24 phones, tablets/foldables, real
split-screen resizing, and three-button navigation remain unverified. Short-IME/cutout
combinations are also simulated; they do not prove every hardware/keyboard combination.
Existing non-spacing lint findings remain outside this change.
No lint warnings were suppressed. Repeat the same device suite on an older phone and
resizable tablet as follow-up coverage, and review reference screenshots when content changes.
