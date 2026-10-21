# Scaling engineering in BananaQ

## Build and visual checks

Gradle build caching, configuration caching, parallel project work, and non-transitive
R classes are enabled. Workers are capped at four, and native-rendered screenshot tests
use one 1.5 GB test process so screenshots do not start several memory-heavy JVMs.

`:feature:scanner:domain` contains Android-independent classification models and rules.
Run `./gradlew :feature:scanner:domain:test` for a fast scanner logic check. The app
depends on that module; changing a layout does not recompile those rules.

`ScalingScreenshotTest` renders actual activity XML on the JVM using Robolectric
native graphics. Its 72 references cover onboarding, home, forms, saved/empty states,
manual/legal pages, and camera/loading/compact/expanded scanner states. Configurations
include normal, enlarged, compact Filipino, landscape, tall phone, and wide tablet. It also checks
frame/control anchoring and that the pinned sheet handle remains outside the scroll
viewport. No camera, TensorFlow interpreter, app startup side effects, or emulator is
needed for these tests.

Reference images live in `app/src/test/screenshots`. Normal test runs compare them
with current renders and fail on a pixel difference. To update them intentionally:

```powershell
./tools/verify-android.ps1 -RecordScreenshots
```

Review the PNGs in `app/build/reports/scaling` before accepting changed references.
Then run `./tools/verify-android.ps1` to verify without replacing them. This helper
also works around long Windows sandbox TEMP paths used by Java's internal sockets.

Commit reviewed reference PNGs together with intentional UI changes, including shared
drawables: a back-chevron edit also changes scanner and detail-page screenshots.
CI stays in verification mode; do not record references in the workflow or skip the
screenshot tests to make a failing check pass.

The GitHub workflow verifies screenshots, unit tests, lint, and the APK with cached
Gradle dependencies. Screenshots use Windows in both local recording and CI to keep
native rendering consistent. Reports are uploaded even when verification fails.

## Lifecycles and memory

`ScannerViewModel` receives a `ScanRepository` through constructor injection. The
Application supplies a ViewModel factory that creates one processor per scanner
owner. Rotation reuses that owner; opening another scanner gets a separate processor.
The interpreter is closed on its inference queue when the owner is cleared. Tests
exercise those ownership and cleanup rules with a fake repository.

The debug leak-monitoring dependency and its notifications have been removed.
Image decoding remains bounded before allocating full pixels, and thumbnails
continue to decode off the main thread.

## Existing offline data behavior

SQLite remains the local source of truth. Scan persistence and its durable sync
outbox precede WorkManager synchronization. Sync requires network connectivity and
uses exponential retry, limited batches, and JPEG-compressed photos. History is
already capped at 200 scans. A Room migration, Paging 3, or request gzip would change
the persistence/server contract and does not correct view geometry; they are not
introduced as part of this layout change.

## Applicability of the proposed techniques

- The app uses XML Views, so Compose `@Stable` annotations do not apply.
- The scanner model must work offline immediately; Play Feature Delivery would add
  a download prerequisite and is not used for this model.
- Baseline Profiles require recorded release journeys and measurement on a connected
  device. JVM screenshots do not generate a valid performance profile. No fabricated
  profile or unmeasured frame-time improvement is claimed here.
- Further vertical modules should follow real feature boundaries after the scanner
  domain separation, rather than moving tightly coupled database/UI code blindly.

Build optimization and memory checks complement responsive constraints; the screenshot
and geometry tests are the part of this change that detects layout scaling regressions.

See `responsive-ui-audit.md` for the app-wide findings, file list, test matrix, and hardware limitations.
