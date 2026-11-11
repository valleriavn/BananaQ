import { after, before, test } from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { PGlite } from '@electric-sql/pglite'

const db = new PGlite()
const ids = {
  admin: '00000000-0000-4000-8000-000000000001',
  mobile: '00000000-0000-4000-8000-000000000002',
  ordinary: '00000000-0000-4000-8000-000000000003',
}
const mobileSchema = new URL('../../supabase/schema.sql', import.meta.url)
const dashboardSchema = new URL('../supabase/dashboard.sql', import.meta.url)

async function signIn(id, role = 'authenticated') {
  await db.exec('reset role')
  await db.exec(role === 'authenticated' ? 'set role authenticated' : 'set role anon')
  await db.query("select set_config('request.jwt.claims', $1, false)", [JSON.stringify({
    sub: id, role, user_metadata: { bananaq_admin: true }, app_metadata: { bananaq_admin: true },
  })])
}

async function rpc(expression, params = []) {
  const { rows } = await db.query('select ' + expression + ' as result', params)
  return rows[0].result
}

before(async () => {
  await db.exec(`
    create role anon nologin;
    create role authenticated nologin;
    create schema auth;
    create table auth.users (
      id uuid primary key, email text, is_anonymous boolean not null, raw_app_meta_data jsonb
    );
    create function auth.jwt() returns jsonb language sql stable as $$
      select coalesce(nullif(current_setting('request.jwt.claims', true), ''), '{}')::jsonb
    $$;
    create function auth.uid() returns uuid language sql stable as $$ select (auth.jwt()->>'sub')::uuid $$;
    insert into auth.users values
      ('${ids.admin}', 'admin@example.invalid', false, '{"bananaq_admin":true}'),
      ('${ids.mobile}', null, true, '{"bananaq_admin":true}'),
      ('${ids.ordinary}', 'ordinary@example.invalid', false, '{}');
  `)
  const schema = (await readFile(mobileSchema, 'utf8')).split('-- Private image storage.')[0]
  await db.exec(schema)
  await db.exec(await readFile(dashboardSchema, 'utf8'))
  await db.exec(await readFile(dashboardSchema, 'utf8')) // Verify reapplying the setup is safe.
  await db.exec(`
    create table public.private_notes (id integer primary key);
    alter table public.private_notes enable row level security;
    grant select on public.private_notes to public;
    create table public.study_participants (id uuid primary key, nickname text);
    alter table public.study_participants enable row level security;
    grant select on public.study_participants to bananaq_dashboard_reader;
    create policy reader on public.study_participants for select to bananaq_dashboard_reader using (true);

    insert into public.user_sessions (session_id, owner_id, started_at, last_active_at)
      values ('10000000-0000-4000-8000-000000000001', '${ids.mobile}', now(), now());
    insert into public.scans values
      ('20000000-0000-4000-8000-000000000001', '${ids.mobile}',
       '10000000-0000-4000-8000-000000000001', null, 'Healthy', 0.93, true, now()),
      ('20000000-0000-4000-8000-000000000002', '${ids.mobile}',
       '10000000-0000-4000-8000-000000000001', null, 'Black Sigatoka', 0.72, true, now());
    insert into public.feedback values
      ('30000000-0000-4000-8000-000000000001', '${ids.mobile}',
       '20000000-0000-4000-8000-000000000001', 'Accurate', 'Sample comment for SQL testing', now()),
      ('30000000-0000-4000-8000-000000000002', '${ids.mobile}',
       '20000000-0000-4000-8000-000000000002', 'Not Sure', 'Different comment', now() - interval '1 day');
  `)
})

after(() => db.close())

test('rejects anonymous sessions and ordinary accounts even with forged request metadata', async () => {
  for (const id of [ids.mobile, ids.ordinary]) {
    await signIn(id)
    await assert.rejects(rpc('public.get_dashboard_schema()'), { code: '42501' })
    await assert.rejects(rpc("public.get_dashboard_data('feedback')"), { code: '42501' })
  }
  await signIn(undefined, 'anon')
  await assert.rejects(rpc('public.get_dashboard_schema()'), { code: '42501' })
})

test('discovers approved tables dynamically with types, keys, and relationships', async () => {
  await signIn(ids.admin)
  const schema = await rpc('public.get_dashboard_schema()')
  assert.deepEqual(schema.map((table) => table.name), ['feedback', 'scans', 'study_participants', 'user_sessions'])
  const feedback = schema.find((table) => table.name === 'feedback')
  assert.deepEqual(feedback.columns.find((column) => column.name === 'feedback_id'), {
    name: 'feedback_id', type: 'uuid', primaryKey: true, nullable: false,
  })
  assert.deepEqual(feedback.relationships.find((key) => key.name === 'feedback_scan_id_fkey'), {
    name: 'feedback_scan_id_fkey', columns: ['scan_id'], targetSchema: 'public',
    targetTable: 'scans', targetColumns: ['scan_id'], readable: true,
  })
  assert.equal(feedback.relationships.find((key) => key.targetSchema === 'auth').readable, false)
  await assert.rejects(db.query('select * from public.feedback'), { code: '42501' })
  await assert.rejects(rpc("public.get_dashboard_data('private_notes')"), { code: '42501' })
})

test('joins verified relationships, sorts, paginates, searches, and filters on the server', async () => {
  await signIn(ids.admin)
  const read = "public.get_dashboard_data('feedback', 'feedback_scan_id_fkey', p_sort => 'submitted_at', p_limit => 1"
  const first = await rpc(read + ')')
  const second = await rpc(read + ', p_offset => 1)')
  assert.equal(first.total, 2)
  assert.equal(first.rows[0].record.accuracy_rating, 'Accurate')
  assert.equal(first.rows[0].related.predicted_disease, 'Healthy')
  assert.equal(second.rows[0].related.predicted_disease, 'Black Sigatoka')
  const filtered = await rpc(read + ", p_search => $1, p_search_fields => '{comments}', p_filters => $2)", [
    'sample COMMENT', { 'related.predicted_disease': 'Healthy' },
  ])
  assert.equal(filtered.total, 1)
  const noMatch = await rpc(read + ", p_search => $1, p_search_fields => '{comments}')", ["' or true --"])
  assert.deepEqual(noMatch, { rows: [], total: 0 })
  await assert.rejects(rpc(read + ", p_filters => '{\"invalid-field\": \"x\"}')"), { code: '22023' })
  await assert.rejects(rpc("public.get_dashboard_data('feedback', 'feedback_owner_id_fkey')"), { code: '42501' })
})

test('aggregates exact totals, valid FK labels, continuous UTC days, and confidence bands', async () => {
  await signIn(ids.admin)
  assert.deepEqual(await rpc("public.get_dashboard_data('feedback', p_mode => 'count')"), { total: 2 })
  const labels = await rpc("public.get_dashboard_data('feedback', 'feedback_scan_id_fkey', 'distribution', 'related.predicted_disease')")
  assert.deepEqual(labels, [{ label: 'Black Sigatoka', count: 1 }, { label: 'Healthy', count: 1 }])
  const daily = await rpc("public.get_dashboard_data('feedback', p_mode => 'daily', p_field => 'submitted_at', p_days => 3)")
  assert.deepEqual(daily.map((day) => day.count), [0, 1, 1])
  const confidence = await rpc("public.get_dashboard_data('scans', p_mode => 'confidence', p_field => 'confidence_score')")
  assert.deepEqual(confidence.map((band) => band.count), [0, 0, 0, 1, 1])
})

test('uses live app metadata so removing admin access takes effect on the next request', async () => {
  await db.exec('reset role')
  await db.query("update auth.users set raw_app_meta_data = '{}' where id = $1", [ids.admin])
  await signIn(ids.admin)
  await assert.rejects(rpc('public.get_dashboard_schema()'), { code: '42501' })
})
