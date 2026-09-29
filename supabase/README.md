# BananaQ Supabase setup

## Dashboard configuration

1. Create or open the Supabase project.
2. Go to **Authentication > Providers > Anonymous Sign-Ins** and enable it.
3. For a public study, configure CAPTCHA or Cloudflare Turnstile and review anonymous-auth rate limits.
4. Open **SQL Editor**, paste the entire contents of `schema.sql`, and run it once. The script is safe to run against the earlier BananaQ schema and creates:
   - owner columns and indexes;
   - authenticated RLS policies;
   - a restricted incremental-sync function;
   - the private `bananaq-scans` Storage bucket and ownership policies.
5. In **Connect**, copy the project URL and publishable key.
6. Add them to the untracked project `local.properties`:

   ```properties
   SUPABASE_URL=https://YOUR_PROJECT_REFERENCE.supabase.co
   SUPABASE_PUBLISHABLE_KEY=YOUR_PUBLISHABLE_KEY
   ```

7. Sync Gradle, rebuild, and run the app. Make one scan, wait for connectivity, then confirm that a user appears under Authentication, owned rows appear in the three tables, and a JPEG appears in that user's folder in Storage.

## Security model

The app has no login screen. Supabase creates one anonymous authenticated identity per installation. Android never supplies an owner ID in its JSON payload; the database function obtains ownership from the verified JWT using `auth.uid()`. The public publishable key is expected in an APK, but it cannot write records without an authenticated session and cannot access another identity's rows or private images.

Do not add a Supabase secret key or `service_role` key to `local.properties`, Gradle, source code, or the APK.

## Existing unowned rows

Rows created by the original public sync function have no trustworthy owner and intentionally remain inaccessible after this migration. Export them first if the research team still needs them. New app records synchronize under authenticated ownership.

## Offline and retry behavior

SQLite remains the source used by the UI. Every mutation enters a local outbox. WorkManager uploads only pending batches while connected and retries transient authentication, Storage, or database failures with exponential backoff. A remote outage never prevents local scanning or feedback.
