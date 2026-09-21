# Android compatibility and scan verification

The app now targets a minimum of **Android 5.0 (API 21)**. Android 1.x–4.x
are not supported by this CameraX/LiteRT dependency stack. A successful build
does not establish that every manufacturer's camera works correctly.

## Scan flow

- Home camera, gallery and detect actions use the same scanner/classifier.
- CameraX binds the preview and full-resolution image capture to the activity
  lifecycle, choosing a rear camera or falling back to a front camera.
- Camera permission denial and devices without a camera still allow image selection.
- The system document picker needs no broad storage/media permission.
- Images are downsampled before decoding and EXIF rotation/mirroring is applied.
- Decoding, model initialization and inference run on a single background queue.
  The interpreter closes on that same queue after pending work finishes.
- Results survive recreation. A scan ID prevents duplicate history records when
  recreation reruns unfinished work; deliberately scanning again creates a new ID.
- History keeps the latest 200 results locally. Feedback is saved locally, not
  uploaded to a server. Disease-library entries are not presented as scans.

## Model contract

The existing model's documented label order and RGB normalization are preserved:
`Black Sigatoka`, `Cordana Leaf Spot`, `Healthy`, `Panama Disease`; RGB / 255.
Input must be a single NHWC RGB image. Output must contain four probabilities.
Float32, uint8 and int8 tensors are supported. Quantization saturates to the
storage range, and invalid/nonfinite outputs fail rather than becoming a diagnosis.

The original confidence thresholds remain 30%, 60%, and 80%. These are not a
validation of disease-detection accuracy or proof that an image contains a banana
leaf. Verify the label order and preprocessing against the training/export files,
and evaluate with labeled leaf and non-leaf photos before relying on predictions.

## Automated checks

Run with JDK 17 or newer and the Android SDK configured in `local.properties`:

```text
gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
python tools/check_native_alignment.py app/build/outputs/apk/debug/app-debug.apk
```

The unit tests cover confidence boundaries, invalid outputs, four-class indexing,
quantization saturation, and memory-bounded image sampling. Device tests check
all bundled disease files and actual inference through the packaged model:

```text
gradlew.bat :app:connectedDebugAndroidTest
```

Native-library ELF alignment is checked separately because an Android version
number alone does not establish 16 KB page-size compatibility. LiteRT 1.4.2 keeps
API 21 support; see [Google's runtime compatibility table](https://developers.google.com/edge/litert/android).

## Device checks still required

Test API 21/22, API 23 (runtime permissions), API 29/30 (storage changes), API 33,
API 35/36, and a 16 KB-page device/emulator. Cover camera permission denial,
permanent denial, rear/front cameras, no flash, picker cancellation, portrait and
landscape EXIF photos, a large photo, invalid image content, repeated captures,
rotation during inference, background/foreground, and process recreation.

No connected Android device was available during this change. Compiling device
tests is not equivalent to running them. Tagalog translation resources are not
bundled; the language screen now explains this instead of silently ignoring selection.
