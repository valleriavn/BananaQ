# BananaQ refactoring report

## Audit and scope

The existing tree already separates data, synchronization, machine learning, and domain models. ScannerActivity and BananaQDatabase remain large; splitting their stateful UI and persistence responsibilities is deferred to a focused change. No duplicate recommendation assets or obsolete model were established. Existing tests, screenshots, model, bilingual JSON, SQL, wrapper, build scripts, release configuration, and useful documentation are retained. No dependencies or SDK/tool versions changed.

## Identity and upgrade compatibility

The Kotlin/Java root package and generated R/BuildConfig namespace are bananaq. See [Android application identity guidance](https://developer.android.com/build/configure-app-module). The explicit applicationId is retained to preserve update and app-data continuity; a single-segment Android application ID is invalid. The retained identity is intentional, not a stale source package. Relative manifest class names resolve through the new namespace. Custom XML views and R8 rules use the new package. The AndroidJUnitRunner remains unchanged; its package belongs to AndroidX.

SyncWorkerFactory resolves persisted worker names derived from the existing installation identity and current worker class names. BananaQApplication provides its configuration, and the manifest removes automatic WorkManager startup so that configuration is used. A launcher alias preserves the original installed launcher component. Database names, versions, preferences, file paths, JSON field names, model preprocessing, labels, and recommendation selection remain unchanged.

## Files renamed or moved

All paths below retain their filenames except AdaptiveSizing.kt, renamed DialogSizing.kt to describe its dialog geometry functions. Package moves preserve existing responsibility subpackages.
- `app/src/androidTest/java/com/example/bananaq/DatabaseIntegrationTest.kt` → `app/src/androidTest/java/bananaq/DatabaseIntegrationTest.kt`
- `app/src/androidTest/java/com/example/bananaq/RecommendationHistoryTest.kt` → `app/src/androidTest/java/bananaq/RecommendationHistoryTest.kt`
- `app/src/androidTest/java/com/example/bananaq/ScalingConfigurationTest.kt` → `app/src/androidTest/java/bananaq/ScalingConfigurationTest.kt`
- `app/src/androidTest/java/com/example/bananaq/ScanIntegrationTest.kt` → `app/src/androidTest/java/bananaq/ScanIntegrationTest.kt`
- `app/src/androidTest/java/com/example/bananaq/ScanResultInteractionTest.kt` → `app/src/androidTest/java/bananaq/ScanResultInteractionTest.kt`
- `app/src/androidTest/java/com/example/bananaq/WindowBehaviorTest.kt` → `app/src/androidTest/java/bananaq/WindowBehaviorTest.kt`
- `app/src/main/java/com/example/bananaq/AccountActivity.kt` → `app/src/main/java/bananaq/AccountActivity.kt`
- `app/src/main/java/com/example/bananaq/AdaptiveSizing.kt` → `app/src/main/java/bananaq/DialogSizing.kt`
- `app/src/main/java/com/example/bananaq/AgreementLocalizer.kt` → `app/src/main/java/bananaq/AgreementLocalizer.kt`
- `app/src/main/java/com/example/bananaq/BananaQApplication.kt` → `app/src/main/java/bananaq/BananaQApplication.kt`
- `app/src/main/java/com/example/bananaq/BottomNavigationRouter.kt` → `app/src/main/java/bananaq/BottomNavigationRouter.kt`
- `app/src/main/java/com/example/bananaq/ContentColumn.kt` → `app/src/main/java/bananaq/ContentColumn.kt`
- `app/src/main/java/com/example/bananaq/DiseaseDetailsActivity.kt` → `app/src/main/java/bananaq/DiseaseDetailsActivity.kt`
- `app/src/main/java/com/example/bananaq/DiseaseLibraryAdapter.kt` → `app/src/main/java/bananaq/DiseaseLibraryAdapter.kt`
- `app/src/main/java/com/example/bananaq/DiseaseNameLocalizer.kt` → `app/src/main/java/bananaq/DiseaseNameLocalizer.kt`
- `app/src/main/java/com/example/bananaq/FeedbackActivity.kt` → `app/src/main/java/bananaq/FeedbackActivity.kt`
- `app/src/main/java/com/example/bananaq/FeedbackDetailActivity.kt` → `app/src/main/java/bananaq/FeedbackDetailActivity.kt`
- `app/src/main/java/com/example/bananaq/GetStartedActivity.kt` → `app/src/main/java/bananaq/GetStartedActivity.kt`
- `app/src/main/java/com/example/bananaq/HistoryActivity.kt` → `app/src/main/java/bananaq/HistoryActivity.kt`
- `app/src/main/java/com/example/bananaq/LanguageActivity.kt` → `app/src/main/java/bananaq/LanguageActivity.kt`
- `app/src/main/java/com/example/bananaq/LeafSwayAnimation.kt` → `app/src/main/java/bananaq/LeafSwayAnimation.kt`
- `app/src/main/java/com/example/bananaq/LocaleAwareActivity.kt` → `app/src/main/java/bananaq/LocaleAwareActivity.kt`
- `app/src/main/java/com/example/bananaq/LocaleHelper.kt` → `app/src/main/java/bananaq/LocaleHelper.kt`
- `app/src/main/java/com/example/bananaq/MainActivity.kt` → `app/src/main/java/bananaq/MainActivity.kt`
- `app/src/main/java/com/example/bananaq/ManualTopicActivity.kt` → `app/src/main/java/bananaq/ManualTopicActivity.kt`
- `app/src/main/java/com/example/bananaq/RaisedBottomNavigationView.kt` → `app/src/main/java/bananaq/RaisedBottomNavigationView.kt`
- `app/src/main/java/com/example/bananaq/ResponsiveActionRow.kt` → `app/src/main/java/bananaq/ResponsiveActionRow.kt`
- `app/src/main/java/com/example/bananaq/ResponsiveDateTimeRow.kt` → `app/src/main/java/bananaq/ResponsiveDateTimeRow.kt`
- `app/src/main/java/com/example/bananaq/ResponsiveFeedbackHeader.kt` → `app/src/main/java/bananaq/ResponsiveFeedbackHeader.kt`
- `app/src/main/java/com/example/bananaq/ResponsiveHeaderRow.kt` → `app/src/main/java/bananaq/ResponsiveHeaderRow.kt`
- `app/src/main/java/com/example/bananaq/ResponsiveScanRow.kt` → `app/src/main/java/bananaq/ResponsiveScanRow.kt`
- `app/src/main/java/com/example/bananaq/ResponsiveTabRow.kt` → `app/src/main/java/bananaq/ResponsiveTabRow.kt`
- `app/src/main/java/com/example/bananaq/ScanHistoryAdapter.kt` → `app/src/main/java/bananaq/ScanHistoryAdapter.kt`
- `app/src/main/java/com/example/bananaq/ScanThumbnailLoader.kt` → `app/src/main/java/bananaq/ScanThumbnailLoader.kt`
- `app/src/main/java/com/example/bananaq/ScannerActivity.kt` → `app/src/main/java/bananaq/ScannerActivity.kt`
- `app/src/main/java/com/example/bananaq/ScannerViewModel.kt` → `app/src/main/java/bananaq/ScannerViewModel.kt`
- `app/src/main/java/com/example/bananaq/SplashActivity.kt` → `app/src/main/java/bananaq/SplashActivity.kt`
- `app/src/main/java/com/example/bananaq/SystemBarInsets.kt` → `app/src/main/java/bananaq/SystemBarInsets.kt`
- `app/src/main/java/com/example/bananaq/UserActionLogger.kt` → `app/src/main/java/bananaq/UserActionLogger.kt`
- `app/src/main/java/com/example/bananaq/UserAgreementActivity.kt` → `app/src/main/java/bananaq/UserAgreementActivity.kt`
- `app/src/main/java/com/example/bananaq/UserManualActivity.kt` → `app/src/main/java/bananaq/UserManualActivity.kt`
- `app/src/main/java/com/example/bananaq/data/AppSessionManager.kt` → `app/src/main/java/bananaq/data/AppSessionManager.kt`
- `app/src/main/java/com/example/bananaq/data/BananaQDatabase.kt` → `app/src/main/java/bananaq/data/BananaQDatabase.kt`
- `app/src/main/java/com/example/bananaq/data/DatabaseRecords.kt` → `app/src/main/java/bananaq/data/DatabaseRecords.kt`
- `app/src/main/java/com/example/bananaq/data/DiseaseRepository.kt` → `app/src/main/java/bananaq/data/DiseaseRepository.kt`
- `app/src/main/java/com/example/bananaq/data/RecommendationProvider.kt` → `app/src/main/java/bananaq/data/RecommendationProvider.kt`
- `app/src/main/java/com/example/bananaq/data/RecommendationSelector.kt` → `app/src/main/java/bananaq/data/RecommendationSelector.kt`
- `app/src/main/java/com/example/bananaq/data/ScanDateGrouping.kt` → `app/src/main/java/bananaq/data/ScanDateGrouping.kt`
- `app/src/main/java/com/example/bananaq/data/ScanHistoryStore.kt` → `app/src/main/java/bananaq/data/ScanHistoryStore.kt`
- `app/src/main/java/com/example/bananaq/data/ScanProcessor.kt` → `app/src/main/java/bananaq/data/ScanProcessor.kt`
- `app/src/main/java/com/example/bananaq/data/ScanRepository.kt` → `app/src/main/java/bananaq/data/ScanRepository.kt`
- `app/src/main/java/com/example/bananaq/data/sync/SupabaseAuthManager.kt` → `app/src/main/java/bananaq/data/sync/SupabaseAuthManager.kt`
- `app/src/main/java/com/example/bananaq/data/sync/SupabaseSyncManager.kt` → `app/src/main/java/bananaq/data/sync/SupabaseSyncManager.kt`
- `app/src/main/java/com/example/bananaq/data/sync/SupabaseSyncWorker.kt` → `app/src/main/java/bananaq/data/sync/SupabaseSyncWorker.kt`
- `app/src/main/java/com/example/bananaq/ml/DiseaseClassifier.kt` → `app/src/main/java/bananaq/ml/DiseaseClassifier.kt`
- `app/src/main/java/com/example/bananaq/ml/ImagePreprocessor.kt` → `app/src/main/java/bananaq/ml/ImagePreprocessor.kt`
- `app/src/main/java/com/example/bananaq/ml/ScanImageLoader.kt` → `app/src/main/java/bananaq/ml/ScanImageLoader.kt`
- `app/src/main/java/com/example/bananaq/ml/TFLiteModel.kt` → `app/src/main/java/bananaq/ml/TFLiteModel.kt`
- `app/src/main/java/com/example/bananaq/model/DiseaseInfo.kt` → `app/src/main/java/bananaq/model/DiseaseInfo.kt`
- `app/src/test/java/com/example/bananaq/RecommendationSelectorTest.kt` → `app/src/test/java/bananaq/RecommendationSelectorTest.kt`
- `app/src/test/java/com/example/bananaq/ResponsiveLayoutTest.kt` → `app/src/test/java/bananaq/ResponsiveLayoutTest.kt`
- `app/src/test/java/com/example/bananaq/ScalingScreenshotTest.kt` → `app/src/test/java/bananaq/ScalingScreenshotTest.kt`
- `app/src/test/java/com/example/bananaq/ScanDateGroupingTest.kt` → `app/src/test/java/bananaq/ScanDateGroupingTest.kt`
- `app/src/test/java/com/example/bananaq/ScanLogicTest.kt` → `app/src/test/java/bananaq/ScanLogicTest.kt`
- `app/src/test/java/com/example/bananaq/ScannerDependencyTest.kt` → `app/src/test/java/bananaq/ScannerDependencyTest.kt`
- `feature/scanner/domain/src/main/kotlin/com/example/bananaq/ml/PredictionScores.kt` → `feature/scanner/domain/src/main/kotlin/bananaq/ml/PredictionScores.kt`
- `feature/scanner/domain/src/main/kotlin/com/example/bananaq/model/ClassificationResult.kt` → `feature/scanner/domain/src/main/kotlin/bananaq/model/ClassificationResult.kt`
- `feature/scanner/domain/src/main/kotlin/com/example/bananaq/model/ConfidenceLevel.kt` → `feature/scanner/domain/src/main/kotlin/bananaq/model/ConfidenceLevel.kt`
- `feature/scanner/domain/src/test/kotlin/com/example/bananaq/ClassificationRulesTest.kt` → `feature/scanner/domain/src/test/kotlin/bananaq/ClassificationRulesTest.kt`

## Other changed files and configuration

- `DOCUMENTATION.md`
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/keepRules/rules.keep`
- `app/src/main/res/layout/activity_account.xml`
- `app/src/main/res/layout/activity_disease_details.xml`
- `app/src/main/res/layout/activity_feedback.xml`
- `app/src/main/res/layout/activity_feedback_detail.xml`
- `app/src/main/res/layout/activity_history.xml`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/res/layout/activity_manual_topic.xml`
- `app/src/main/res/layout/activity_scanner.xml`
- `app/src/main/res/layout/activity_user_agreement.xml`
- `app/src/main/res/layout/activity_user_manual.xml`
- `app/src/main/res/layout/dialog_avatar_picker.xml`
- `app/src/main/res/layout/dialog_feedback_confirmation.xml`
- `app/src/main/res/layout/dialog_unidentified_scan.xml`
- `app/src/main/res/layout/include_account_brand_header.xml`
- `app/src/main/res/layout/item_history_scan.xml`
- `app/src/main/res/values/ids.xml`
- `docs/responsive-ui-audit.md`
- `docs/ui-spacing-audit.md`
- Added `app/src/main/java/bananaq/data/sync/SyncWorkerFactory.kt`.
- Added `app/src/test/java/bananaq/SyncWorkerFactoryTest.kt`: legacy/current job construction retains UUID, input data, and retry count; unrelated workers fall back; application supplies the factory.
- Added this report.

## Cleanup and deletion decisions

Removed unused imports from AccountActivity (ViewGroup), FeedbackDetailActivity (Uri, ViewGroup), HistoryActivity (ViewGroup), and ResponsiveLayoutTest (Rect, LinearLayout). Removed the redundant language alias in DiseaseRepository. Removed the unreferenced largeTextOnboardingScroll resource declaration after searching source, layouts, tests, and documentation. No existing source or asset files were deleted; paths removed by the migration have corresponding replacements above.

Baseline lint reports 42 unused-resource candidates, including legacy colors, manual text, animations, and disease advice strings. They are retained for individual review; this task explicitly preserves recommendation content and UI design. Lint suggestions about target SDK, dependency upgrades, synchronous identity/migration preference writes, and UI geometry were not applied as cleanup because they can change compatibility or behavior.

- The resource `R.color.brown_primary` appears to be unused
- The resource `R.color.brown_dark` appears to be unused
- The resource `R.color.brown_light` appears to be unused
- The resource `R.color.brown_background` appears to be unused
- The resource `R.dimen.home_disease_carousel_height` appears to be unused
- The resource `R.drawable.ic_manual_arrow` appears to be unused
- The resource `R.drawable.ic_manual_back` appears to be unused
- The resource `R.string.manual_heading` appears to be unused
- The resource `R.string.manual_intro` appears to be unused
- The resource `R.string.getting_started` appears to be unused
- The resource `R.string.important_information` appears to be unused
- The resource `R.string.capture_title` appears to be unused
- The resource `R.string.capture_description` appears to be unused
- The resource `R.string.analyze_title` appears to be unused
- The resource `R.string.analyze_description` appears to be unused
- The resource `R.string.results_title` appears to be unused
- The resource `R.string.results_description` appears to be unused
- The resource `R.string.legal_privacy` appears to be unused
- The resource `R.string.legal_not_available` appears to be unused
- The resource `R.string.settings_section` appears to be unused
- The resource `R.string.guest_name` appears to be unused
- The resource `R.string.guest_description` appears to be unused
- The resource `R.string.avatar_section` appears to be unused
- The resource `R.string.choose_avatar` appears to be unused
- The resource `R.string.avatar_hint` appears to be unused
- The resource `R.string.settings_label` appears to be unused
- The resource `R.string.logout_label` appears to be unused
- The resource `R.string.account_unavailable` appears to be unused
- The resource `R.anim.nav_fade_enter` appears to be unused
- The resource `R.anim.nav_fade_exit` appears to be unused
- The resource `R.anim.page_back_enter` appears to be unused
- The resource `R.anim.page_back_exit` appears to be unused
- The resource `R.dimen.spacing_48` appears to be unused
- The resource `R.string.summary_additional_comments` appears to be unused
- The resource `R.string.disease_treatment` appears to be unused
- The resource `R.string.treatment_black_sigatoka` appears to be unused
- The resource `R.string.prevention_black_sigatoka` appears to be unused
- The resource `R.string.treatment_panama` appears to be unused
- The resource `R.string.prevention_panama` appears to be unused
- The resource `R.string.treatment_cordana` appears to be unused
- The resource `R.string.prevention_cordana` appears to be unused
- The resource `R.style.BananaQWindowAnimation` appears to be unused

## Validation

- PASS: `:app:assembleDebug :app:assembleDebugAndroidTest :app:assembleRelease` completed successfully (final verification: 130 tasks; 11 executed, 119 up-to-date). Release shrinking/minification and release vital lint passed. Release signing is not configured, so the release APK is unsigned.
- PASS: domain tests: 3/3. App tests: 36/42, including all 3 new worker migration tests, 10 bilingual recommendation tests, scan logic, dependency/lifecycle, date grouping, and responsive-layout checks.
- FAILED: all 6 screenshot cases have pixel differences against existing checked-in baselines. These baselines were preserved. An isolated original-HEAD run reproduced all 6 failures with identical pixel-difference counts (4618, 740, 1228, 7716, 740, 426), confirming they predate this refactor. All 21 PNGs generated before those assertions are byte-for-byte identical between HEAD and the refactored project.
- PASS: full `:app:lintDebug --max-workers=1` completed successfully with 0 errors and 226 warnings. The initial parallel run crashed inside AndroidX RepeatOnLifecycleDetector; running with one worker resolved the tool failure without disabling detectors or changing dependencies. Original HEAD also has 0 errors and 226 warnings. A temporary added provider-export warning was resolved by explicitly declaring exported=false, matching the existing merged-manifest value. Existing overridePendingTransition deprecation warnings remain.
- PASS: all 8 recommendation JSON assets parse. All 9 assets match the Git index, including the previously staged model. Model-loading constants, preprocessing, label order, prediction calculations, and JSON content are unchanged.
- PASS: compared all 69 moved sources to HEAD after normalizing package references; only the six documented files have additional edits. The manifest resolves the application and activities in bananaq, keeps the installed identity, exposes the legacy launcher alias, and removes default WorkManager startup. The release R8 mapping retains the worker name.
- PASS: final reference search finds the old dotted package string only in the intentional applicationId entry; old source parents are removed. Git diff whitespace validation passed.
- NOT RUN: connected instrumentation tests and physical upgrade/data-retention/navigation/camera/model-inference checks; ADB reports no connected device or emulator.
- Build logs: `build/refactor-validation.log` (initial lint/test failures), `build/refactor-final-build.log` (final tests), `build/refactor-apk-build.log` (successful APK assembly), `build/refactor-final-lint.log` (successful full lint retry), `build/refactor-final-verification.log` (final lint plus all three APK builds), and `build/refactor-baseline-validation.log` (isolated original-HEAD comparison).
- APKs: `app/build/outputs/apk/debug/BananaQ.apk`, `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`, `app/build/outputs/apk/release/BananaQ.apk`.

## Preserved user work

The TFLite model was already staged as modified before this task. It was not edited, staged, or reverted. No changes were committed or pushed.

## Remaining concerns

The six pre-existing screenshot baseline mismatches need visual review as a separate change; no baselines were re-recorded. Full debug lint and release vital lint passed. Existing lint warnings remain for focused follow-up. Device tests must still verify camera/model inference, navigation, and an installed-app upgrade with existing scan history, recommendation snapshots, and queued sync work. Large scanner/database classes remain candidates for a dedicated architectural refactor.

## Git summary

The package migration and cleanup are unstaged. Git status shows old/new paths separately until the renames are staged. The only pre-existing staged change is the model. No commits or pushes were made. Complete status snapshot follows:

```text
 M DOCUMENTATION.md
 M app/build.gradle.kts
 D app/src/androidTest/java/com/example/bananaq/DatabaseIntegrationTest.kt
 D app/src/androidTest/java/com/example/bananaq/RecommendationHistoryTest.kt
 D app/src/androidTest/java/com/example/bananaq/ScalingConfigurationTest.kt
 D app/src/androidTest/java/com/example/bananaq/ScanIntegrationTest.kt
 D app/src/androidTest/java/com/example/bananaq/ScanResultInteractionTest.kt
 D app/src/androidTest/java/com/example/bananaq/WindowBehaviorTest.kt
 M app/src/main/AndroidManifest.xml
M  app/src/main/assets/diseases/BananaQ_MobileNetV2_Final.tflite
 D app/src/main/java/com/example/bananaq/AccountActivity.kt
 D app/src/main/java/com/example/bananaq/AdaptiveSizing.kt
 D app/src/main/java/com/example/bananaq/AgreementLocalizer.kt
 D app/src/main/java/com/example/bananaq/BananaQApplication.kt
 D app/src/main/java/com/example/bananaq/BottomNavigationRouter.kt
 D app/src/main/java/com/example/bananaq/ContentColumn.kt
 D app/src/main/java/com/example/bananaq/DiseaseDetailsActivity.kt
 D app/src/main/java/com/example/bananaq/DiseaseLibraryAdapter.kt
 D app/src/main/java/com/example/bananaq/DiseaseNameLocalizer.kt
 D app/src/main/java/com/example/bananaq/FeedbackActivity.kt
 D app/src/main/java/com/example/bananaq/FeedbackDetailActivity.kt
 D app/src/main/java/com/example/bananaq/GetStartedActivity.kt
 D app/src/main/java/com/example/bananaq/HistoryActivity.kt
 D app/src/main/java/com/example/bananaq/LanguageActivity.kt
 D app/src/main/java/com/example/bananaq/LeafSwayAnimation.kt
 D app/src/main/java/com/example/bananaq/LocaleAwareActivity.kt
 D app/src/main/java/com/example/bananaq/LocaleHelper.kt
 D app/src/main/java/com/example/bananaq/MainActivity.kt
 D app/src/main/java/com/example/bananaq/ManualTopicActivity.kt
 D app/src/main/java/com/example/bananaq/RaisedBottomNavigationView.kt
 D app/src/main/java/com/example/bananaq/ResponsiveActionRow.kt
 D app/src/main/java/com/example/bananaq/ResponsiveDateTimeRow.kt
 D app/src/main/java/com/example/bananaq/ResponsiveFeedbackHeader.kt
 D app/src/main/java/com/example/bananaq/ResponsiveHeaderRow.kt
 D app/src/main/java/com/example/bananaq/ResponsiveScanRow.kt
 D app/src/main/java/com/example/bananaq/ResponsiveTabRow.kt
 D app/src/main/java/com/example/bananaq/ScanHistoryAdapter.kt
 D app/src/main/java/com/example/bananaq/ScanThumbnailLoader.kt
 D app/src/main/java/com/example/bananaq/ScannerActivity.kt
 D app/src/main/java/com/example/bananaq/ScannerViewModel.kt
 D app/src/main/java/com/example/bananaq/SplashActivity.kt
 D app/src/main/java/com/example/bananaq/SystemBarInsets.kt
 D app/src/main/java/com/example/bananaq/UserActionLogger.kt
 D app/src/main/java/com/example/bananaq/UserAgreementActivity.kt
 D app/src/main/java/com/example/bananaq/UserManualActivity.kt
 D app/src/main/java/com/example/bananaq/data/AppSessionManager.kt
 D app/src/main/java/com/example/bananaq/data/BananaQDatabase.kt
 D app/src/main/java/com/example/bananaq/data/DatabaseRecords.kt
 D app/src/main/java/com/example/bananaq/data/DiseaseRepository.kt
 D app/src/main/java/com/example/bananaq/data/RecommendationProvider.kt
 D app/src/main/java/com/example/bananaq/data/RecommendationSelector.kt
 D app/src/main/java/com/example/bananaq/data/ScanDateGrouping.kt
 D app/src/main/java/com/example/bananaq/data/ScanHistoryStore.kt
 D app/src/main/java/com/example/bananaq/data/ScanProcessor.kt
 D app/src/main/java/com/example/bananaq/data/ScanRepository.kt
 D app/src/main/java/com/example/bananaq/data/sync/SupabaseAuthManager.kt
 D app/src/main/java/com/example/bananaq/data/sync/SupabaseSyncManager.kt
 D app/src/main/java/com/example/bananaq/data/sync/SupabaseSyncWorker.kt
 D app/src/main/java/com/example/bananaq/ml/DiseaseClassifier.kt
 D app/src/main/java/com/example/bananaq/ml/ImagePreprocessor.kt
 D app/src/main/java/com/example/bananaq/ml/ScanImageLoader.kt
 D app/src/main/java/com/example/bananaq/ml/TFLiteModel.kt
 D app/src/main/java/com/example/bananaq/model/DiseaseInfo.kt
 M app/src/main/keepRules/rules.keep
 M app/src/main/res/layout/activity_account.xml
 M app/src/main/res/layout/activity_disease_details.xml
 M app/src/main/res/layout/activity_feedback.xml
 M app/src/main/res/layout/activity_feedback_detail.xml
 M app/src/main/res/layout/activity_history.xml
 M app/src/main/res/layout/activity_main.xml
 M app/src/main/res/layout/activity_manual_topic.xml
 M app/src/main/res/layout/activity_scanner.xml
 M app/src/main/res/layout/activity_user_agreement.xml
 M app/src/main/res/layout/activity_user_manual.xml
 M app/src/main/res/layout/dialog_avatar_picker.xml
 M app/src/main/res/layout/dialog_feedback_confirmation.xml
 M app/src/main/res/layout/dialog_unidentified_scan.xml
 M app/src/main/res/layout/include_account_brand_header.xml
 M app/src/main/res/layout/item_history_scan.xml
 M app/src/main/res/values/ids.xml
 D app/src/test/java/com/example/bananaq/RecommendationSelectorTest.kt
 D app/src/test/java/com/example/bananaq/ResponsiveLayoutTest.kt
 D app/src/test/java/com/example/bananaq/ScalingScreenshotTest.kt
 D app/src/test/java/com/example/bananaq/ScanDateGroupingTest.kt
 D app/src/test/java/com/example/bananaq/ScanLogicTest.kt
 D app/src/test/java/com/example/bananaq/ScannerDependencyTest.kt
 M docs/responsive-ui-audit.md
 M docs/ui-spacing-audit.md
 D feature/scanner/domain/src/main/kotlin/com/example/bananaq/ml/PredictionScores.kt
 D feature/scanner/domain/src/main/kotlin/com/example/bananaq/model/ClassificationResult.kt
 D feature/scanner/domain/src/main/kotlin/com/example/bananaq/model/ConfidenceLevel.kt
 D feature/scanner/domain/src/test/kotlin/com/example/bananaq/ClassificationRulesTest.kt
?? app/src/androidTest/java/bananaq/DatabaseIntegrationTest.kt
?? app/src/androidTest/java/bananaq/RecommendationHistoryTest.kt
?? app/src/androidTest/java/bananaq/ScalingConfigurationTest.kt
?? app/src/androidTest/java/bananaq/ScanIntegrationTest.kt
?? app/src/androidTest/java/bananaq/ScanResultInteractionTest.kt
?? app/src/androidTest/java/bananaq/WindowBehaviorTest.kt
?? app/src/main/java/bananaq/AccountActivity.kt
?? app/src/main/java/bananaq/AgreementLocalizer.kt
?? app/src/main/java/bananaq/BananaQApplication.kt
?? app/src/main/java/bananaq/BottomNavigationRouter.kt
?? app/src/main/java/bananaq/ContentColumn.kt
?? app/src/main/java/bananaq/DialogSizing.kt
?? app/src/main/java/bananaq/DiseaseDetailsActivity.kt
?? app/src/main/java/bananaq/DiseaseLibraryAdapter.kt
?? app/src/main/java/bananaq/DiseaseNameLocalizer.kt
?? app/src/main/java/bananaq/FeedbackActivity.kt
?? app/src/main/java/bananaq/FeedbackDetailActivity.kt
?? app/src/main/java/bananaq/GetStartedActivity.kt
?? app/src/main/java/bananaq/HistoryActivity.kt
?? app/src/main/java/bananaq/LanguageActivity.kt
?? app/src/main/java/bananaq/LeafSwayAnimation.kt
?? app/src/main/java/bananaq/LocaleAwareActivity.kt
?? app/src/main/java/bananaq/LocaleHelper.kt
?? app/src/main/java/bananaq/MainActivity.kt
?? app/src/main/java/bananaq/ManualTopicActivity.kt
?? app/src/main/java/bananaq/RaisedBottomNavigationView.kt
?? app/src/main/java/bananaq/ResponsiveActionRow.kt
?? app/src/main/java/bananaq/ResponsiveDateTimeRow.kt
?? app/src/main/java/bananaq/ResponsiveFeedbackHeader.kt
?? app/src/main/java/bananaq/ResponsiveHeaderRow.kt
?? app/src/main/java/bananaq/ResponsiveScanRow.kt
?? app/src/main/java/bananaq/ResponsiveTabRow.kt
?? app/src/main/java/bananaq/ScanHistoryAdapter.kt
?? app/src/main/java/bananaq/ScanThumbnailLoader.kt
?? app/src/main/java/bananaq/ScannerActivity.kt
?? app/src/main/java/bananaq/ScannerViewModel.kt
?? app/src/main/java/bananaq/SplashActivity.kt
?? app/src/main/java/bananaq/SystemBarInsets.kt
?? app/src/main/java/bananaq/UserActionLogger.kt
?? app/src/main/java/bananaq/UserAgreementActivity.kt
?? app/src/main/java/bananaq/UserManualActivity.kt
?? app/src/main/java/bananaq/data/AppSessionManager.kt
?? app/src/main/java/bananaq/data/BananaQDatabase.kt
?? app/src/main/java/bananaq/data/DatabaseRecords.kt
?? app/src/main/java/bananaq/data/DiseaseRepository.kt
?? app/src/main/java/bananaq/data/RecommendationProvider.kt
?? app/src/main/java/bananaq/data/RecommendationSelector.kt
?? app/src/main/java/bananaq/data/ScanDateGrouping.kt
?? app/src/main/java/bananaq/data/ScanHistoryStore.kt
?? app/src/main/java/bananaq/data/ScanProcessor.kt
?? app/src/main/java/bananaq/data/ScanRepository.kt
?? app/src/main/java/bananaq/data/sync/SupabaseAuthManager.kt
?? app/src/main/java/bananaq/data/sync/SupabaseSyncManager.kt
?? app/src/main/java/bananaq/data/sync/SupabaseSyncWorker.kt
?? app/src/main/java/bananaq/data/sync/SyncWorkerFactory.kt
?? app/src/main/java/bananaq/ml/DiseaseClassifier.kt
?? app/src/main/java/bananaq/ml/ImagePreprocessor.kt
?? app/src/main/java/bananaq/ml/ScanImageLoader.kt
?? app/src/main/java/bananaq/ml/TFLiteModel.kt
?? app/src/main/java/bananaq/model/DiseaseInfo.kt
?? app/src/test/java/bananaq/RecommendationSelectorTest.kt
?? app/src/test/java/bananaq/ResponsiveLayoutTest.kt
?? app/src/test/java/bananaq/ScalingScreenshotTest.kt
?? app/src/test/java/bananaq/ScanDateGroupingTest.kt
?? app/src/test/java/bananaq/ScanLogicTest.kt
?? app/src/test/java/bananaq/ScannerDependencyTest.kt
?? app/src/test/java/bananaq/SyncWorkerFactoryTest.kt
?? docs/refactoring-report.md
?? feature/scanner/domain/src/main/kotlin/bananaq/ml/PredictionScores.kt
?? feature/scanner/domain/src/main/kotlin/bananaq/model/ClassificationResult.kt
?? feature/scanner/domain/src/main/kotlin/bananaq/model/ConfidenceLevel.kt
?? feature/scanner/domain/src/test/kotlin/bananaq/ClassificationRulesTest.kt
```
