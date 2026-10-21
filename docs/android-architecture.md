# Android scan and layout architecture

## Scanning (MVVM)

`ScannerActivity` owns permission requests, CameraX, the image picker, and rendering.
`ScannerViewModel` owns the running scan and exposes `Idle`, `Processing`, `Success`,
and `Failure` through lifecycle-aware `LiveData`. It retains the job across Activity
recreation and never holds an Activity, View, or preview bitmap.

The Application's factory injects a `ScanRepository` into each scanner ViewModel.
Classification models and probability rules live in `:feature:scanner:domain`,
which has no Android dependency and can be tested independently of the APK.

`ScanProcessor` owns image decoding, inference, and history persistence. Its model is
created, used, and closed on the ViewModel's single executor. Cleanup is queued after
inference so the interpreter cannot be closed while running. The Activity's separate
preview loader checks that its URI is still selected before displaying a bitmap.

The shipped model is `diseases/BananaQ_MobileNetV2_Final.tflite`. Its input is float32
`[1,224,224,3]`, named `rgb_0_to_1`; the graph contains the subsequent `x * 2 - 1`
normalization. The app must supply RGB values divided by 255, not normalize twice.
Its output has four probabilities. The configured label order is still Black
Sigatoka, Cordana Leaf Spot, Healthy, Panama Disease. The model contains no bundled
label file: confirm this order against the training export before assessing accuracy.

## Layout ownership

`LocaleAwareActivity` changes locale only. It does not override Android font scale or
density and does not insert, remove, or resize screen views.

- XML owns constraints, padding, minimum touch sizes, and scroll containers.
- Geometry uses dp; text uses sp. Text containers use wrap_content.
- Home, onboarding, and scan details declare their scroll containers directly.
- Scan details have one vertical scroll owner for the header, tabs, and body.
- Scanner controls stay anchored to the window bottom. Result summaries do not
  participate in camera layout constraints, and hidden camera chrome reserves
  its measured space. Expanded details start at the preview guideline.
- Compact scan results open with a tap. Native BottomSheetBehavior handles drag
  dismissal and returns to the compact result when hidden. The pinned handle
  stays available while the details scroll; tapping it is an accessible close action.
- `ResponsiveHeaderRow` moves language controls below branding when measured text
  cannot fit. `ResponsiveDateTimeRow` similarly stacks the date and clock.
- Responsive rows decide their orientation during measurement, not in a layout
  callback that changes the view hierarchy after layout.
- Dialog widths use the usable Activity window; avatar columns use remaining width
  after sheet padding. Decorative onboarding spacing uses height resource qualifiers.

## Regression checks

`ScanIntegrationTest` loads the shipped model, checks its input/output contract,
and runs inference. `ScalingConfigurationTest` checks locale wrapping, large text,
and page layouts at compact, landscape, and tablet sizes in English and Tagalog.
It also checks scanner anchor stability as the result appears at different font
scales and densities. `ScanResultInteractionTest` exercises tap-to-open, drag-to-close, and the
accessible close action.

Run `testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`, then
`connectedDebugAndroidTest` with a connected device/emulator. Instrumentation layout
checks complement a visual check with system font/display sizing and navigation insets;
they do not establish classifier accuracy on real-world images.

For emulator-free visual checks and cached CI builds, see [scaling engineering](scaling-engineering.md).
