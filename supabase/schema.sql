create table if not exists public.user_sessions (
    session_id uuid primary key,
    owner_id uuid references auth.users(id) on delete cascade,
    started_at timestamptz not null,
    last_active_at timestamptz not null,
    ended_at timestamptz,
    is_active boolean not null default true,
    constraint user_sessions_time_order check (
        last_active_at >= started_at and (ended_at is null or ended_at >= started_at)
    )
);

create table if not exists public.scans (
    scan_id uuid primary key,
    owner_id uuid references auth.users(id) on delete cascade,
    session_id uuid not null references public.user_sessions(session_id) on delete cascade,
    image_path text,
    predicted_disease text,
    confidence_score real,
    is_banana_leaf boolean not null,
    scanned_at timestamptz not null,
    constraint scans_confidence_range check (
        confidence_score is null or confidence_score between 0 and 1
    ),
    constraint scans_prediction_consistency check (
        (is_banana_leaf and predicted_disease is not null and confidence_score is not null)
        or (not is_banana_leaf and predicted_disease is null and confidence_score is null)
    )
);

create table if not exists public.feedback (
    feedback_id uuid primary key,
    owner_id uuid references auth.users(id) on delete cascade,
    scan_id uuid not null unique references public.scans(scan_id) on delete cascade,
    accuracy_rating text not null,
    comments text not null default '',
    submitted_at timestamptz not null,
    constraint feedback_rating_allowed check (
        accuracy_rating in ('Very Accurate', 'Accurate', 'Not Sure', 'Inaccurate', 'Very Inaccurate')
    ),
    constraint feedback_comments_length check (char_length(comments) <= 2000)
);

alter table public.user_sessions add column if not exists owner_id uuid references auth.users(id) on delete cascade;
alter table public.scans add column if not exists owner_id uuid references auth.users(id) on delete cascade;
alter table public.feedback add column if not exists owner_id uuid references auth.users(id) on delete cascade;

create index if not exists user_sessions_owner_id_idx on public.user_sessions(owner_id);
create index if not exists scans_owner_id_idx on public.scans(owner_id);
create index if not exists scans_session_id_idx on public.scans(session_id);
create index if not exists scans_scanned_at_idx on public.scans(scanned_at desc);
create index if not exists feedback_owner_id_idx on public.feedback(owner_id);
create index if not exists feedback_submitted_at_idx on public.feedback(submitted_at desc);

alter table public.user_sessions enable row level security;
alter table public.scans enable row level security;
alter table public.feedback enable row level security;

drop policy if exists user_sessions_owner_policy on public.user_sessions;
create policy user_sessions_owner_policy on public.user_sessions
    for all to authenticated
    using (owner_id = (select auth.uid()))
    with check (owner_id = (select auth.uid()));

drop policy if exists scans_owner_policy on public.scans;
create policy scans_owner_policy on public.scans
    for all to authenticated
    using (owner_id = (select auth.uid()))
    with check (owner_id = (select auth.uid()));

drop policy if exists feedback_owner_policy on public.feedback;
create policy feedback_owner_policy on public.feedback
    for all to authenticated
    using (owner_id = (select auth.uid()))
    with check (owner_id = (select auth.uid()));

revoke all on table public.user_sessions from anon, authenticated;
revoke all on table public.scans from anon, authenticated;
revoke all on table public.feedback from anon, authenticated;

create or replace function public.sync_bananaq_records(
    p_sessions jsonb default '[]'::jsonb,
    p_scans jsonb default '[]'::jsonb,
    p_feedback jsonb default '[]'::jsonb
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_owner uuid := auth.uid();
    session_count integer := 0;
    scan_count integer := 0;
    feedback_count integer := 0;
begin
    if v_owner is null then raise exception 'Authentication required'; end if;
    if jsonb_typeof(p_sessions) <> 'array'
       or jsonb_typeof(p_scans) <> 'array'
       or jsonb_typeof(p_feedback) <> 'array' then
        raise exception 'Sync inputs must be JSON arrays';
    end if;
    if jsonb_array_length(p_sessions) > 200
       or jsonb_array_length(p_scans) > 200
       or jsonb_array_length(p_feedback) > 200 then
        raise exception 'Sync payload exceeds the allowed record count';
    end if;

    insert into public.user_sessions (
        session_id, owner_id, started_at, last_active_at, ended_at, is_active
    )
    select session_id, v_owner, started_at, last_active_at, ended_at, is_active
    from jsonb_to_recordset(p_sessions) as item(
        session_id uuid, started_at timestamptz, last_active_at timestamptz,
        ended_at timestamptz, is_active boolean
    )
    where session_id is not null and started_at is not null and last_active_at is not null
    on conflict (session_id) do update set
        started_at = excluded.started_at,
        last_active_at = excluded.last_active_at,
        ended_at = excluded.ended_at,
        is_active = excluded.is_active
    where public.user_sessions.owner_id = v_owner;
    get diagnostics session_count = row_count;

    insert into public.scans (
        scan_id, owner_id, session_id, image_path, predicted_disease,
        confidence_score, is_banana_leaf, scanned_at
    )
    select item.scan_id, v_owner, item.session_id, item.image_path,
           item.predicted_disease, item.confidence_score, item.is_banana_leaf, item.scanned_at
    from jsonb_to_recordset(p_scans) as item(
        scan_id uuid, session_id uuid, image_path text, predicted_disease text,
        confidence_score real, is_banana_leaf boolean, scanned_at timestamptz
    )
    join public.user_sessions session
      on session.session_id = item.session_id and session.owner_id = v_owner
    where item.scan_id is not null and item.is_banana_leaf is not null
      and item.scanned_at is not null
    on conflict (scan_id) do update set
        session_id = excluded.session_id,
        image_path = excluded.image_path,
        predicted_disease = excluded.predicted_disease,
        confidence_score = excluded.confidence_score,
        is_banana_leaf = excluded.is_banana_leaf,
        scanned_at = excluded.scanned_at
    where public.scans.owner_id = v_owner;
    get diagnostics scan_count = row_count;

    insert into public.feedback (
        feedback_id, owner_id, scan_id, accuracy_rating, comments, submitted_at
    )
    select item.feedback_id, v_owner, item.scan_id, item.accuracy_rating,
           coalesce(item.comments, ''), item.submitted_at
    from jsonb_to_recordset(p_feedback) as item(
        feedback_id uuid, scan_id uuid, accuracy_rating text,
        comments text, submitted_at timestamptz
    )
    join public.scans scan on scan.scan_id = item.scan_id and scan.owner_id = v_owner
    where item.feedback_id is not null and item.accuracy_rating is not null
      and item.submitted_at is not null
    on conflict (scan_id) do update set
        accuracy_rating = excluded.accuracy_rating,
        comments = excluded.comments,
        submitted_at = excluded.submitted_at
    where public.feedback.owner_id = v_owner;
    get diagnostics feedback_count = row_count;

    return jsonb_build_object(
        'sessions', session_count, 'scans', scan_count, 'feedback', feedback_count
    );
end;
$$;

revoke all on function public.sync_bananaq_records(jsonb, jsonb, jsonb) from public, anon;
grant execute on function public.sync_bananaq_records(jsonb, jsonb, jsonb) to authenticated;

-- Private image storage. Each authenticated installation owns one top-level folder.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('bananaq-scans', 'bananaq-scans', false, 5242880, array['image/jpeg'])
on conflict (id) do update set
    public = false,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists bananaq_scan_images_insert on storage.objects;
create policy bananaq_scan_images_insert on storage.objects
    for insert to authenticated
    with check (
        bucket_id = 'bananaq-scans'
        and (storage.foldername(name))[1] = (select auth.uid())::text
    );

drop policy if exists bananaq_scan_images_select on storage.objects;
create policy bananaq_scan_images_select on storage.objects
    for select to authenticated
    using (
        bucket_id = 'bananaq-scans'
        and owner_id = (select auth.jwt()->>'sub')
    );

drop policy if exists bananaq_scan_images_update on storage.objects;
create policy bananaq_scan_images_update on storage.objects
    for update to authenticated
    using (
        bucket_id = 'bananaq-scans'
        and owner_id = (select auth.jwt()->>'sub')
    )
    with check (
        bucket_id = 'bananaq-scans'
        and (storage.foldername(name))[1] = (select auth.uid())::text
    );
