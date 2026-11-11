# BananaQ Admin

BananaQ Admin is a read-only React dashboard for the BananaQ banana leaf disease study. It uses the same Supabase project as the Android app and runs independently from it.

## Local development

Install Node.js 24 and the pnpm version pinned in `package.json`. Run these commands from `admin-dashboard/`.

```powershell
Copy-Item .env.example .env.local
pnpm install --frozen-lockfile
pnpm dev
```

Set these values in `.env.local` before starting the server. Copy them from the project's **Connect** dialog.

| Variable | Value |
| --- | --- |
| `VITE_SUPABASE_URL` | HTTPS Supabase project URL |
| `VITE_SUPABASE_ANON_KEY` | Supabase publishable key or legacy anon key |

The login screen explains missing or invalid settings. Production builds fail before bundling if the configuration is invalid. Vite includes the public key and project URL in the frontend, so use only the publishable or legacy anon key. Never put a service-role key, a Supabase secret key, a database password, or an administrator password into a `VITE_` variable.

## Supabase setup

The live project must be configured by its owner before administrators can read data. Review [dashboard.sql](supabase/dashboard.sql) and the table grants before running it in **SQL Editor**. It adds read-only dashboard functions, a dedicated reader role, and SELECT policies for that role. It creates no study tables. The existing mobile sync function and its grants are independent.

1. Create a dedicated email/password user in **Authentication → Users**, or use an existing dedicated administrator account. Verify its email in the project's normal account flow.
2. Run `supabase/dashboard.sql` from this dashboard folder in the Supabase SQL Editor as the project's database administrator. The script expects the existing BananaQ tables to have RLS enabled. Keep `bananaq_dashboard_private` out of the exposed Data API schemas.
3. Copy the administrator account's UUID from **Authentication → Users**. In SQL Editor, replace the placeholder and run this role assignment. It uses server-managed Auth app metadata.

```sql
update auth.users
set raw_app_meta_data = coalesce(raw_app_meta_data, '{}'::jsonb)
    || '{"bananaq_admin": true}'::jsonb
where id = '<administrator-user-uuid>'::uuid
  and is_anonymous is false
  and email is not null
returning id;
```

One returned row confirms the assignment. To revoke access, update that account's `raw_app_meta_data` with `raw_app_meta_data - 'bananaq_admin'`. The next dashboard request will check the current Auth record. Sign in to the dashboard with the dedicated account. An ordinary or anonymous mobile account should receive an access error.

### Administrator permissions

The browser uses email/password authentication with a persisted Supabase session. The server checks the authenticated account, rejects anonymous accounts, and reads the role from the current Auth record on every dashboard request. User-editable metadata has no effect on authorization.

Public RPC wrappers run with caller permissions and delegate to private read functions. Those functions execute as a dedicated `NOLOGIN NOBYPASSRLS` role. It receives direct SELECT grants and SELECT policies only on reviewed tables. Data queries use validated catalog identifiers, bounded pagination, and read-only functions. The dashboard has no row-editing operations.

### Discovery and approved tables

  Discovery reads the live PostgreSQL catalogs. It returns tables, column data types, nullable flags, primary-key membership, and ordered foreign-key column pairs. It includes public tables with RLS enabled and a direct SELECT grant to `bananaq_dashboard_reader`. A relationship to an unapproved table is metadata only. Explorer lists approved tables dynamically and obtains exact counts for the selected table and active filters.

The installer initially approves the existing RLS-enabled `user_sessions`, `scans`, and `feedback` tables. For each additional table, the project owner should review all columns and approve access explicitly. For example, after confirming that a real `public.study_participants` table already exists with RLS enabled and is safe to expose to study administrators, use:

```sql
grant select on public.study_participants to bananaq_dashboard_reader;
create policy bananaq_dashboard_read on public.study_participants
  for select to bananaq_dashboard_reader using (true);
```

Use **Refresh** in Database Explorer to discover newly approved tables. Discovery covers approved public tables, not private application or Supabase system schemas. If the RPC has not been installed, the dashboard displays a setup error rather than a sample schema.

The existing Android repository defines these columns. The public-key connection does not reveal whether the hosted database has extra columns or tables, so the live catalog remains authoritative.

| Repository table | Columns |
| --- | --- |
| `public.user_sessions` | `session_id` UUID PK, `owner_id` UUID nullable, `started_at` and `last_active_at` timestamptz, `ended_at` timestamptz nullable, `is_active` boolean |
| `public.scans` | `scan_id` UUID PK, `owner_id` UUID nullable, `session_id` UUID, `image_path` and `predicted_disease` text nullable, `confidence_score` real nullable, `is_banana_leaf` boolean, `scanned_at` timestamptz |
| `public.feedback` | `feedback_id` UUID PK, `owner_id` UUID nullable, `scan_id` UUID unique, `accuracy_rating` and `comments` text, `submitted_at` timestamptz |

Repository foreign keys link feedback to scans and scans to user sessions. The three `owner_id` columns refer to `auth.users.id`. Authentication users are kept outside the dashboard's record browser. Installation owners and session IDs do not establish participant identities.

### Measurements

- Scans, feedback submissions, participants, and sessions are counted separately from their discovered tables. Participant totals and navigation require a suitable approved participant table.
- Feedback filters use fields found on the feedback record or its verified scan/prediction foreign key. The main feedback table omits owner and session identifiers.
- The Android `is_banana_leaf` flag describes the scan input. Prediction-validity metrics require a separate discovered validity field.
- Daily charts use UTC date boundaries and the selected period, including dates with zero records. Submission dates are taken from recorded timestamps, so delayed sync appears on the original submission date.
- Label charts show up to 12 values with recorded labels. Confidence charts use the repository's 0–1 score or a discovered `confidence_percent` field, shown in 20 percentage-point bands. Missing confidence values are excluded.
- The dashboard queries pages and aggregated results from Supabase. It does not retrieve the entire study dataset into the browser.

## Android theme

The dashboard uses a resized copy of the Android logo from `drawable-nodpi/logo.png` and the Android palette in `src/theme.css`. The palette comes from `values/colors.xml` and the semantic mappings from `values/themes.xml`. Warm cream, ivory, and soft yellow borders match the existing app. A dark leaf-green sidebar uses the app's heading color, and the yellow controls follow the agreement and rounded button styles.

Cards use the 20 dp container radius and 1 dp outline from `dimensions.xml` and `DiseaseLibraryCard`. Buttons use the pill shape from `button_styles.xml`. Web inputs use a 12 px radius to suit denser forms. The web type stack uses Roboto when installed and falls back to the system sans-serif. Web title sizes scale above the 22 sp mobile heading on wide screens. Sidebar width, table spacing, chart typography, and responsive breakpoints are web layout decisions. The Android resources are read-only references for this work.

## Build and test

```text
pnpm typecheck
pnpm test:sql
pnpm build
pnpm preview
```

The SQL tests run against a disposable PostgreSQL engine. They load the Android repository schema and use isolated test data to exercise the reader role, ordinary and anonymous account denial, catalog discovery, verified joins, pagination, filtering, totals, and chart aggregates. They do not access or write to the live project.

## GitHub Pages

The Vite base is `/BananaQ/`. React Router uses hash routes so bookmarked pages can refresh on static hosting.

1. In the GitHub repository, set **Settings → Pages → Source** to **GitHub Actions**.
2. Add `VITE_SUPABASE_URL` and `VITE_SUPABASE_ANON_KEY` as repository Actions variables or secrets. The compiled static site exposes those public values by design.
3. Copy [the workflow template](deployment/bananaq-admin-pages.yml) to `.github/workflows/bananaq-admin-pages.yml` at the repository root. GitHub runs workflows only from that location. The template remains inside this folder to keep the dashboard implementation within the requested boundary.
4. Commit the workflow and dashboard source, then use **Actions → Deploy BananaQ Admin to GitHub Pages → Run workflow**. Subsequent pushes to `main` that change dashboard files trigger deployment.
5. Open the URL from the deployment job and test sign-in with an approved account. The workflow tests the SQL and builds only `admin-dashboard/`, then deploys its `dist/` directory.

GitHub Pages serves static assets. Authorization and study record access remain in Supabase.
