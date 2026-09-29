# BananaQ Technical and Operational Guide

## Product overview

BananaQ is an offline-first Android application that classifies banana-leaf images as Black Sigatoka, Cordana Leaf Spot, Healthy, or Panama Disease. It runs its TensorFlow Lite model on the device, stores scan history locally, and synchronizes consented research records and private scan images to Supabase when a connection is available.

The confidence score describes the model's strength of preference among its supported classes. It is not a guarantee of diagnostic correctness. Low-confidence or ambiguous images are shown as unable to identify; the current model does not contain a dedicated non-banana class.

## Technology

- Kotlin and Android XML views
- Minimum Android 7.0 (API 24), target API 35
- CameraX for camera capture
- LiteRT/TensorFlow Lite for on-device inference
- SQLite for the offline source of truth
- WorkManager for durable network retries
- Supabase Auth, Postgres, and private Storage for remote synchronization

## Main user flow

1. Select English or Tagalog and accept the user agreement.
2. Open Scan and capture a leaf or choose an image.
3. BananaQ processes the image locally and presents a prediction when confidence and margin thresholds are met.
4. Open the result for symptoms, treatment guidance, and prevention information.
5. Review previous scans in History and optionally submit one feedback response per scan.

## Data flow

1. Every app foreground session creates or updates a local `user_sessions` record.
2. A scan is compressed into `files/scan_photos` and its metadata is written to SQLite.
3. Session, scan, and feedback mutations add an entry to `sync_outbox`.
4. WorkManager waits for network connectivity, signs the installation into Supabase anonymously, uploads pending images to the private `bananaq-scans` bucket, and invokes the owner-scoped sync function.
5. Successfully synchronized outbox entries are removed. Failed work is retried with exponential backoff.
6. Only the latest 200 scans are retained locally. Managed image files belonging to pruned records are removed.

No email, phone number, selected avatar, or selected language is stored in the research database. The selected language and avatar are device-only preferences.

## Project structure

- `com.example.bananaq`: activities, localization, shared bottom navigation, and reusable UI utilities
- `com.example.bananaq.data`: SQLite records, session lifecycle, disease content, and scan history
- `com.example.bananaq.data.sync`: anonymous authentication, incremental WorkManager sync, Storage uploads
- `com.example.bananaq.ml`: model loading, preprocessing, output validation, and classification
- `com.example.bananaq.model`: domain models
- `assets/diseases` and `assets/diseases_tl`: model and localized disease content
- `supabase/schema.sql`: database function, RLS ownership policies, and private Storage setup

## Build configuration

Copy the values from `supabase.properties.example` into the untracked `local.properties` file:

```properties
SUPABASE_URL=https://YOUR_PROJECT_REFERENCE.supabase.co
SUPABASE_PUBLISHABLE_KEY=YOUR_PUBLISHABLE_KEY
```

Never place a Supabase secret or service-role key in the Android project. See `supabase/README.md` for dashboard setup.

## Quality checks

Run before producing an APK:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleRelease
```

Manual checks should include a 320dp phone, a typical phone, tablet, landscape, 200% font size, TalkBack, offline scanning, reconnect-and-sync, camera denial, process recreation, and English/Tagalog switching.

## Release checklist

- Run the current `supabase/schema.sql` and enable Anonymous Sign-Ins.
- Configure CAPTCHA/Turnstile and review Supabase Auth rate limits before public distribution.
- Review user agreement and privacy wording with the research adviser.
- Verify that private images and SQLite data are excluded from Android backup.
- Validate the model on a held-out field dataset and record per-class precision, recall, F1, and confusion matrix.
- Update `versionCode` and `versionName` for every distributed build.
- Configure a protected release signing key outside the repository.
- Run unit, instrumentation, lint, and release builds.

## Known model limitation

The bundled classifier has four banana-condition classes and no explicit non-banana class. Confidence rejection reduces uncertain outputs but cannot guarantee rejection of unrelated high-confidence images. Resolving that limitation requires model and dataset work rather than UI-only logic.
