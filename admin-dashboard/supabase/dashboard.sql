-- Review the table grants below before running in the Supabase SQL Editor.
begin;

do $setup$
begin
  if not exists (select 1 from pg_catalog.pg_roles where rolname = 'bananaq_dashboard_reader') then
    create role bananaq_dashboard_reader nologin noinherit nobypassrls;
  end if;
  if exists (
    select 1 from pg_catalog.pg_roles where rolname = 'bananaq_dashboard_reader'
      and (rolcanlogin or rolsuper or rolbypassrls or rolcreaterole or rolcreatedb or rolreplication)
  ) then
    raise exception 'Existing bananaq_dashboard_reader role has unexpected privileges';
  end if;
  execute format('grant bananaq_dashboard_reader to %I', current_user);
end
$setup$;

create schema if not exists bananaq_dashboard_private;
revoke all on schema bananaq_dashboard_private from public, anon, authenticated;
grant usage on schema bananaq_dashboard_private to authenticated, bananaq_dashboard_reader;
grant create on schema bananaq_dashboard_private to bananaq_dashboard_reader;

-- Keep this helper installer-owned. It checks the current Auth account on each request.
create or replace function bananaq_dashboard_private.require_admin()
returns void language plpgsql stable security definer set search_path = ''
as $fn$
begin
  if not exists (
    select 1 from auth.users
    where id = auth.uid()
      and auth.jwt()->>'role' = 'authenticated'
      and is_anonymous is false
      and email is not null
      and raw_app_meta_data->'bananaq_admin' = 'true'::jsonb
  ) then
    raise exception 'BananaQ administrator access required' using errcode = '42501';
  end if;
end
$fn$;
revoke all on function bananaq_dashboard_private.require_admin() from public, anon, authenticated;
grant execute on function bananaq_dashboard_private.require_admin() to bananaq_dashboard_reader;

-- The reader has SELECT access only to explicitly reviewed, RLS-enabled study tables.
-- These grants and policies affect only the dedicated NOLOGIN role, not the mobile API roles.
do $approve$
declare
  relation_name text;
begin
  foreach relation_name in array array['user_sessions', 'scans', 'feedback'] loop
    if exists (
      select 1 from pg_catalog.pg_class c join pg_catalog.pg_namespace n on n.oid = c.relnamespace
      where n.nspname = 'public' and c.relname = relation_name
        and c.relkind in ('r', 'p') and c.relrowsecurity
    ) then
      execute format('grant select on public.%I to bananaq_dashboard_reader', relation_name);
      execute format('drop policy if exists bananaq_dashboard_read on public.%I', relation_name);
      execute format('create policy bananaq_dashboard_read on public.%I for select to bananaq_dashboard_reader using (true)', relation_name);
    end if;
  end loop;
end
$approve$;
grant usage on schema public to bananaq_dashboard_reader;

create or replace function bananaq_dashboard_private.approved_tables()
returns table(table_id oid, table_name name)
language sql stable security invoker set search_path = ''
as $fn$
  select c.oid, c.relname
  from pg_catalog.pg_class c
  join pg_catalog.pg_namespace n on n.oid = c.relnamespace
  where n.nspname = 'public' and c.relkind in ('r', 'p') and c.relrowsecurity
    and exists (
      select 1 from pg_catalog.aclexplode(c.relacl) acl
      where acl.grantee = current_user::regrole and acl.privilege_type = 'SELECT'
    )
$fn$;
revoke all on function bananaq_dashboard_private.approved_tables() from public, anon, authenticated;
grant execute on function bananaq_dashboard_private.approved_tables() to bananaq_dashboard_reader;

create or replace function bananaq_dashboard_private.discover()
returns jsonb language plpgsql stable security definer set search_path = ''
as $fn$
declare
  result jsonb;
begin
  perform bananaq_dashboard_private.require_admin();
  select coalesce(jsonb_agg(jsonb_build_object(
    'name', t.table_name,
    'columns', coalesce((
      select jsonb_agg(jsonb_build_object(
        'name', a.attname, 'type', pg_catalog.format_type(a.atttypid, a.atttypmod),
        'nullable', not (a.attnotnull or typ.typnotnull), 'primaryKey', exists (
          select 1 from pg_catalog.pg_constraint pk
          where pk.conrelid = t.table_id and pk.contype = 'p' and a.attnum = any(pk.conkey)
        )
      ) order by a.attnum)
      from pg_catalog.pg_attribute a
      join pg_catalog.pg_type typ on typ.oid = a.atttypid
      where a.attrelid = t.table_id and a.attnum > 0 and not a.attisdropped
    ), '[]'::jsonb),
    'relationships', coalesce((
      select jsonb_agg(jsonb_build_object(
        'name', fk.conname, 'targetSchema', n.nspname, 'targetTable', target.relname,
        'columns', (select jsonb_agg(src.attname order by keys.ord)
          from unnest(fk.conkey) with ordinality as keys(num, ord)
          join pg_catalog.pg_attribute src on src.attrelid = t.table_id and src.attnum = keys.num),
        'targetColumns', (select jsonb_agg(dst.attname order by keys.ord)
          from unnest(fk.confkey) with ordinality as keys(num, ord)
          join pg_catalog.pg_attribute dst on dst.attrelid = target.oid and dst.attnum = keys.num),
        'readable', exists (select 1 from bananaq_dashboard_private.approved_tables() allowed
          where allowed.table_id = target.oid)
      ) order by fk.conname)
      from pg_catalog.pg_constraint fk
      join pg_catalog.pg_class target on target.oid = fk.confrelid
      join pg_catalog.pg_namespace n on n.oid = target.relnamespace
      where fk.conrelid = t.table_id and fk.contype = 'f'
    ), '[]'::jsonb)
  ) order by t.table_name), '[]'::jsonb) into result
  from bananaq_dashboard_private.approved_tables() t;
  return result;
end
$fn$;
alter function bananaq_dashboard_private.discover() owner to bananaq_dashboard_reader;

-- Resolve a field to a quoted SQL expression only after checking the catalog.
create or replace function bananaq_dashboard_private.column_ref(base_id oid, related_id oid, field text)
returns text language plpgsql stable security invoker set search_path = ''
as $fn$
declare
  source_id oid := base_id;
  column_name text := field;
  relation_alias text := 'b';
begin
  if left(field, 8) = 'related.' then
    source_id := related_id;
    column_name := substr(field, 9);
    relation_alias := 'r';
  end if;
  if not exists (
    select 1 from pg_catalog.pg_attribute
    where attrelid = source_id and attname = column_name and attnum > 0 and not attisdropped
  ) then
    raise exception 'Unknown dashboard column' using errcode = '22023';
  end if;
  return format('%I.%I', relation_alias, column_name);
end
$fn$;
revoke all on function bananaq_dashboard_private.column_ref(oid, oid, text) from public, anon, authenticated;
grant execute on function bananaq_dashboard_private.column_ref(oid, oid, text) to bananaq_dashboard_reader;

create or replace function bananaq_dashboard_private.read_study(
  p_table text, p_fk text, p_mode text, p_field text, p_search text, p_search_fields text[],
  p_filters jsonb, p_sort text, p_desc boolean, p_limit integer, p_offset integer,
  p_days integer, p_epoch_ms boolean, p_percent boolean
)
returns jsonb language plpgsql stable security definer set search_path = ''
as $fn$
declare
  base_id oid;
  related_id oid;
  related_name name;
  join_condition text;
  from_sql text;
  where_sql text := ' where true';
  search_columns text;
  row_sql text := 'jsonb_build_object(''record'', to_jsonb(b), ''related'', null)';
  sort_sql text;
  key_order text;
  field_sql text;
  date_sql text;
  numeric_sql text;
  filter_key text;
  filter_value text;
  total bigint;
  result jsonb;
  row_limit integer := least(greatest(coalesce(p_limit, 25), 1), 100);
  row_offset integer := greatest(coalesce(p_offset, 0), 0);
  days integer := least(greatest(coalesce(p_days, 30), 1), 366);
begin
  perform bananaq_dashboard_private.require_admin();
  select table_id into base_id from bananaq_dashboard_private.approved_tables() where table_name = p_table;
  if base_id is null then
    raise exception 'Table is not approved for dashboard access' using errcode = '42501';
  end if;
  from_sql := format(' from public.%I as b', p_table);

  if p_fk is not null then
    select fk.confrelid, allowed.table_name, (
      select string_agg(format('b.%I = r.%I', src.attname, dst.attname), ' and ')
      from unnest(fk.conkey, fk.confkey) as keys(src_num, dst_num)
      join pg_catalog.pg_attribute src on src.attrelid = base_id and src.attnum = keys.src_num
      join pg_catalog.pg_attribute dst on dst.attrelid = fk.confrelid and dst.attnum = keys.dst_num
    ) into related_id, related_name, join_condition
    from pg_catalog.pg_constraint fk
    join bananaq_dashboard_private.approved_tables() allowed on allowed.table_id = fk.confrelid
    where fk.conrelid = base_id and fk.contype = 'f' and fk.conname = p_fk;
    if join_condition is null then
      raise exception 'Relationship is not approved for dashboard access' using errcode = '42501';
    end if;
    from_sql := from_sql || format(' left join public.%I as r on %s', related_name, join_condition);
    row_sql := 'jsonb_build_object(''record'', to_jsonb(b), ''related'', to_jsonb(r))';
  end if;

  if coalesce(p_search, '') <> '' then
    if length(p_search) > 200 or cardinality(p_search_fields) > 24 then
      raise exception 'Search exceeds dashboard limits' using errcode = '22023';
    end if;
    select string_agg(bananaq_dashboard_private.column_ref(base_id, related_id, item) || '::text', ', ')
      into search_columns from unnest(p_search_fields) as item;
    if search_columns is not null then
      where_sql := where_sql || format(
        ' and strpos(lower(concat_ws('' '', %s)), lower(%L)) > 0', search_columns, p_search
      );
    end if;
  end if;

  if jsonb_typeof(coalesce(p_filters, '{}'::jsonb)) <> 'object'
    or (select count(*) from jsonb_object_keys(coalesce(p_filters, '{}'::jsonb))) > 8 then
    raise exception 'Invalid dashboard filters' using errcode = '22023';
  end if;
  for filter_key, filter_value in select key, value from jsonb_each_text(p_filters) loop
    field_sql := bananaq_dashboard_private.column_ref(base_id, related_id, filter_key);
    where_sql := where_sql || format(' and %s::text = %L', field_sql, filter_value);
  end loop;

  if p_mode in ('records', 'count') then
    execute 'select count(*)' || from_sql || where_sql into total;
    if p_mode = 'count' then return jsonb_build_object('total', total); end if;
    select string_agg(format('b.%I', a.attname), ', ' order by keys.ord) into key_order
    from pg_catalog.pg_constraint pk
    cross join lateral unnest(pk.conkey) with ordinality as keys(num, ord)
    join pg_catalog.pg_attribute a on a.attrelid = base_id and a.attnum = keys.num
    where pk.conrelid = base_id and pk.contype = 'p';
    sort_sql := coalesce(key_order, 'b.tableoid, b.ctid');
    if p_sort is not null then
      field_sql := bananaq_dashboard_private.column_ref(base_id, related_id, p_sort);
      sort_sql := field_sql || (case when p_desc then ' desc' else ' asc' end) || ' nulls last, ' || sort_sql;
    end if;
    execute format(
      'select coalesce(jsonb_agg(page.row_data order by page.row_number), ''[]''::jsonb)
       from (select %s as row_data, row_number() over (order by %s) as row_number%s%s
         order by %s limit %s offset %s) page',
      row_sql, sort_sql, from_sql, where_sql, sort_sql, row_limit, row_offset
    ) into result;
    return jsonb_build_object('rows', result, 'total', total);
  end if;

  field_sql := bananaq_dashboard_private.column_ref(base_id, related_id, p_field);
  if p_mode = 'distribution' then
    execute format(
      'select coalesce(jsonb_agg(to_jsonb(items) order by items.count desc, items.label), ''[]''::jsonb)
       from (select %1$s::text as label, count(*) as count%2$s%3$s and %1$s::text <> ''''
         group by 1 order by 2 desc, 1 limit %4$s) items',
      field_sql, from_sql, where_sql, row_limit
    ) into result;
  elsif p_mode = 'daily' then
    date_sql := case when p_epoch_ms then format(
      '(to_timestamp(case when %1$s::double precision between -62135596800000 and 253402300799999
        then %1$s::double precision / 1000 end) at time zone ''UTC'')::date', field_sql
    ) else format('(%s::timestamptz at time zone ''UTC'')::date', field_sql) end;
    execute format(
      'select coalesce(jsonb_agg(jsonb_build_object(''label'', days.day::date, ''count'', coalesce(totals.count, 0))
        order by days.day), ''[]''::jsonb)
       from generate_series((now() at time zone ''UTC'')::date - (%1$s - 1),
         (now() at time zone ''UTC'')::date, interval ''1 day'') days(day)
       left join (select %2$s as day, count(*) as count%3$s%4$s
         and %2$s >= (now() at time zone ''UTC'')::date - (%1$s - 1) group by 1) totals
       on totals.day = days.day::date', days, date_sql, from_sql, where_sql
    ) into result;
  elsif p_mode = 'confidence' then
    numeric_sql := format('(%s::double precision * %s)', field_sql, case when p_percent then 1 else 100 end);
    execute format(
      'select coalesce(jsonb_agg(jsonb_build_object(''label'', bands.band * 20 || ''–'' || (bands.band + 1) * 20 || ''%%'',
        ''count'', coalesce(totals.count, 0)) order by bands.band), ''[]''::jsonb)
       from generate_series(0, 4) bands(band)
       left join (select least(4, floor(%1$s / 20)::integer) as band, count(*) as count%2$s%3$s
         and %1$s between 0 and 100 group by 1) totals on totals.band = bands.band',
      numeric_sql, from_sql, where_sql
    ) into result;
  else
    raise exception 'Unsupported dashboard query mode' using errcode = '22023';
  end if;
  return result;
end
$fn$;
alter function bananaq_dashboard_private.read_study(text,text,text,text,text,text[],jsonb,text,boolean,integer,integer,integer,boolean,boolean)
  owner to bananaq_dashboard_reader;

-- Public API wrappers run as the caller. Private functions enforce the administrator check.
create or replace function public.get_dashboard_schema()
returns jsonb language sql stable security invoker set search_path = ''
as $fn$ select bananaq_dashboard_private.discover() $fn$;

create or replace function public.get_dashboard_data(
  p_table text, p_fk text default null, p_mode text default 'records', p_field text default null,
  p_search text default null, p_search_fields text[] default '{}', p_filters jsonb default '{}',
  p_sort text default null, p_desc boolean default true, p_limit integer default 25,
  p_offset integer default 0, p_days integer default 30, p_epoch_ms boolean default false,
  p_percent boolean default false
)
returns jsonb language sql stable security invoker set search_path = ''
as $fn$
  select bananaq_dashboard_private.read_study(p_table, p_fk, p_mode, p_field, p_search,
    p_search_fields, p_filters, p_sort, p_desc, p_limit, p_offset, p_days, p_epoch_ms, p_percent)
$fn$;

revoke all on function bananaq_dashboard_private.discover() from public, anon, authenticated;
revoke all on function bananaq_dashboard_private.read_study(text,text,text,text,text,text[],jsonb,text,boolean,integer,integer,integer,boolean,boolean) from public, anon, authenticated;
revoke all on function public.get_dashboard_schema() from public, anon, authenticated;
revoke all on function public.get_dashboard_data(text,text,text,text,text,text[],jsonb,text,boolean,integer,integer,integer,boolean,boolean) from public, anon, authenticated;
grant execute on function bananaq_dashboard_private.discover() to authenticated;
grant execute on function bananaq_dashboard_private.read_study(text,text,text,text,text,text[],jsonb,text,boolean,integer,integer,integer,boolean,boolean) to authenticated;
grant execute on function public.get_dashboard_schema() to authenticated;
grant execute on function public.get_dashboard_data(text,text,text,text,text,text[],jsonb,text,boolean,integer,integer,integer,boolean,boolean) to authenticated;
revoke create on schema bananaq_dashboard_private from bananaq_dashboard_reader;

notify pgrst, 'reload schema';
commit;
