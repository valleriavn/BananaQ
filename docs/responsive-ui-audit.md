# Responsive UI audit — 21 October 2026

## Project and scope

BananaQ is a native Android app written in Kotlin, using XML Views, ConstraintLayout,
AppCompat, Material components, RecyclerView, and CameraX. Minimum SDK is 24;
compile/target SDK is 35. The existing branding, colors, content, navigation,
classification, local persistence, and network contracts are retained.

The audit covered all 12 activity layouts, reusable headers/navigation, list cards,
four custom dialogs, styles/dimensions, system-inset handling, image loading, and
scanner/form state changes. Layout spacing uses dp and text uses sp. Locale wrapping
preserves Android's density and font settings; no global scaling override is used.

## Root causes and fixes

| Component | Root cause | Implemented fix |
| --- | --- | --- |
| Language selection | Asymmetric label margins centered text only in the area beside the flag. | Equal start/end reservations center the labels in the whole pill; a bounded 240dp choice width accommodates large text. |
| Welcome illustration | Fixed 240dp height and default centered vertical bias made the farmer small and too high on tall phones. | Height resources follow available window height; bottom bias and a 12dp bottom margin place a larger, undistorted image lower. Short pages can scroll. |
| Shared headers | Oversized title and vertical gaps, plus insufficient width for large text. | Shared 24sp brand and 10dp vertical spacing; existing content-based stacking retained. Language controls preserve 48dp targets. |
| Feedback scan summary | A fixed 142dp thumbnail consumed most of a narrow card's text width. | `ResponsiveFeedbackHeader` stacks metadata below the image when native text and image cannot fit side by side. |
| History and feedback empty states | Cards had no bottom constraint and could extend beneath navigation. | Empty-state containers now scroll within the same constrained viewport as populated lists. |
| Wide reading surfaces | Bodies stretched across the entire large window. | A shared 720dp maximum bounds text-heavy content; columns stay centered inside available parent constraints. |
| Scan result heading | Large confidence and disease fields squeezed the scientific name. | The existing adaptive header stacks metadata when needed; scientific names keep a readable width. |
| Paired actions | Equal-width columns could break single-word labels at large font scales. | `ResponsiveActionRow` stacks agreement and dialog actions when their native labels cannot fit; button appearance and handlers are retained. |
| Scanner loading | Replacing a multiline instruction with a shorter processing label changed frame constraints. | A shared instruction container reserves the larger native text layout for both states. Frame and control anchors stay unchanged. |
| Scanner/avatars/confirmation | Several controls had 24–46dp or 30dp interactive bounds. | 48dp targets for drag handles, result tabs, gallery/flash, avatar edit, and confirmation actions. Icon padding preserves the avatar edit icon's visual size. |
| Custom dialogs | Width adapted, but maximum height relied on platform window measurement. | Scrollable dialogs explicitly fit the usable activity height, system bars, cutouts, and keyboard, without counting already-applied padding twice. |
| Repeated insets | Navigation layout parameters were reassigned even when unchanged. | Repeated delivery no longer schedules redundant navigation resizing; system/IME insets remain handled once at the root. |
| Missing images | A logo placeholder inherited photo cropping. | Fallback uses `FIT_CENTER`; successfully decoded scan photos use `CENTER_CROP`. Decoding remains bounded and asynchronous. |

## Files changed in this pass

- `app/src/main/java/com/example/bananaq/ContentColumn.kt`: shared maximum reading width.
- `app/src/main/java/com/example/bananaq/ResponsiveFeedbackHeader.kt`: adaptive image/metadata arrangement.
- `app/src/main/java/com/example/bananaq/ResponsiveActionRow.kt`: content-based action stacking without shrinking text.
- `app/src/main/java/com/example/bananaq/AdaptiveSizing.kt`: usable-window dialog height/width.
- `AccountActivity.kt`, `HistoryActivity.kt`, `FeedbackDetailActivity.kt` in the same Java package: apply bounded dialog sizing.
- `SystemBarInsets.kt`, `RaisedBottomNavigationView.kt`: prevent repeated inset resize work.
- `ScanThumbnailLoader.kt`: preserve placeholder logo aspect ratio.
- `ScannerActivity.kt`: show/hide the complete stable instruction container.
- `app/src/main/res/layout/activity_language.xml`, `activity_get_started.xml`: centered choices and lower/larger farmer.
- `activity_main.xml`, `activity_account.xml`, `activity_user_agreement.xml`, `activity_manual_topic.xml`, `activity_user_manual.xml`, `activity_disease_details.xml`: bounded readable bodies.
- `activity_feedback_detail.xml`: adaptive scan metadata, bounded form and saved state, correct fallback fit.
- `activity_history.xml`, `activity_feedback.xml`: bounded empty/list viewports.
- `activity_scanner.xml`: stable instructions and accessible control bounds.
- `dialog_feedback_confirmation.xml`, `dialog_unidentified_scan.xml`, `dialog_avatar_picker.xml`: adaptive action rows; confirmation actions also receive padded 48dp targets.
- `app/src/main/res/values/dimensions.xml`, `values-h600dp/dimensions.xml`, `values-h840dp/dimensions.xml`: shared reading width, choice width, and height-based illustration sizes.
- `app/src/main/res/values/manual_styles.xml`: shared native-sized scanner instruction text.
- `app/src/test/java/com/example/bananaq/ResponsiveLayoutTest.kt`: populated layouts, saved feedback, history rows, short dialogs.
- `app/src/test/java/com/example/bananaq/ScalingScreenshotTest.kt` and screenshot PNGs: broader visual coverage and loading-state anchor assertions.
- `app/src/androidTest/java/com/example/bananaq/WindowBehaviorTest.kt`: real keyboard, dialog actions, repeated insets, and device renders.

## Automated verification

`ResponsiveLayoutTest` covers 72 configurations: six windows (320×480, 360×800,
411×890, 640×360, 600×960, 1024×768 dp), three densities (160/320/480 dpi), two font
scales (100%/200%), and English/Filipino. Eleven populated pages are checked in each
configuration, with additional saved-feedback, history-row, and short-dialog cases.
Tests check text bounds, truncation, positive scroll viewports, bounded scrolling,
and image/metadata arrangement. These are simulated layouts, not 72 physical devices.

Native JVM screenshots cover home, onboarding, scanner camera/loading/summary/details,
plus account, feedback form/saved state, history/feedback empty states, manual pages,
agreement/actions, and disease details. There are 72 reference screenshots. Six screen configurations include a tall
411×890dp phone and 1024×768dp wide window. Reference PNGs are in
`app/src/test/screenshots`; current renders are in `app/build/reports/scaling`.

Final automated results: 42 JVM tests passed (39 app tests and three scanner-domain
rules tests). All 72 screenshot comparisons passed without recording. Lint and both
debug APK builds succeeded. Lint has zero errors and 241 warnings, including existing
hardcoded content, small secondary text, unused resources, and dependency suggestions;
no lint findings were suppressed to obtain a passing build.

Build/check commands:

```powershell
./tools/verify-android.ps1
./tools/verify-android.ps1 -Tasks @(':app:assembleDebugAndroidTest')
```

Record intentionally changed references with `-RecordScreenshots`, review the renders,
then verify without recording. Debug APK: `app/build/outputs/apk/debug/BananaQ.apk`.

## Device verification and limits

Connected device: Samsung SM-A166P, Android API 36, 1080×2340 physical pixels,
420dpi display override (450dpi physical), font scale 1.08. The final APK was installed with `adb install -r` without clearing app data. All 12
instrumented tests passed: keyboard/input/submit visibility, avatar-dialog actions,
repeated system-bar/cutout/IME insets, actual onboarding/home captures, scaling and
locale preservation, model inference, and result tap/drag interaction. Device captures
are under `app/build/reports/scaling/device`; the runner log is
`app/build/reports/device-ui-tests.txt`. The keyboard was actually opened on this
phone; synthetic inset assertions do not represent physically switching navigation
modes.

No emulator was used. API 24 devices, actual tablets/foldables, three-button navigation,
physical display cutouts other than this phone, and real split-screen resizing have
not been exercised on hardware. Cutout/IME/bar combinations are also checked through
synthetic inset delivery. Automated long content checks do not substitute for every
possible translation or API payload. Live backend outages and capture accuracy are
outside the layout audit; model-loading/inference and result gestures have their
separate integration checks.

Recommended follow-up: repeat the device suite on a low-memory API 24 phone and a
resizable tablet, and review screenshots whenever visible content or typography changes.

## Home library follow-up

The home brand is now 20sp with 6dp vertical header spacing and a 28dp logo.
The language switch uses inset backgrounds for a smaller visible pill while retaining
48dp touch targets. The greeting is 22sp and the Disease Library heading is 20sp.
Library cards use 14sp labels in compact 128dp base-width cards with 72dp images,
with width adapting for native font scaling. The library section sits near the bottom
of the filled home viewport with 24dp outer spacing above navigation. The illustration region
absorbs unused height through its layout weight, preserving the image aspect ratio;
short windows retain the illustration minimum height and natural vertical scrolling.

The three diseases repeat from a centered virtual position, allowing swipes in either
direction without reaching an end or jumping back to the start. There is no snap helper
or visible recentering. Image bitmaps are shared across repeated cards, and two cycles
of card views are cached. The initial card stays aligned with the content inset and
padding clips neighboring cards. Snapshot fixtures populate the actual looping adapter;
checks cover the bottom spacing, and device tests cover first-card alignment, navigation,
and smooth scrolling across cycle boundaries in both directions.

Follow-up verification: nine device tests passed for window behavior, library alignment/navigation, and scaling. The updated APK is installed on the Samsung phone.
