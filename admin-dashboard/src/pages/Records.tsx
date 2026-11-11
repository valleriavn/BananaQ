import { useCallback, useMemo, useState } from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { RecordTable } from '../components/RecordTable'
import type { Facet } from '../components/RecordTable'
import { Empty, ErrorState, Loading, PageHeading } from '../components/Ui'
import { useRequest } from '../hooks/useRequest'
import type { Page, Row } from '../lib/data'
import { getData, valueOf } from '../lib/data'
import { countFormat, formatCell } from '../lib/format'
import { labelField, toFields } from '../lib/schema'
import type { Relationship, Table, View } from '../lib/schema'

const field = (view: View, key: string) => view.fields.find((item) => item.key === key)
const searchable = (view: View) => view.fields.filter((column) => /^(text|character|uuid|citext)/.test(column.type))

export function FeedbackPage({ view }: { view: View }) {
  const columns = [
    ...labelField(view.date, 'Submitted'),
    ...labelField(view.label, 'Predicted label'),
    ...labelField(view.rating, 'Rating'),
    ...labelField(view.result, 'Valid prediction'),
    ...labelField(view.comment, 'Comment'),
  ]
  const facets: Facet[] = []
  if (view.label) facets.push({ field: view.label, label: 'Predicted label' })
  if (view.rating) facets.push({ field: view.rating, label: 'Rating' })
  if (view.result) facets.push({ field: view.result, label: 'Valid prediction' })
  const searchFields = [view.label, view.rating, view.comment].filter((item) => item !== undefined)
  const safeFallback = view.fields.filter((item) => !item.primaryKey && !/(^id$|_id$|email|phone|token)/i.test(item.name)).slice(0, 5)
  return <>
    <PageHeading title="Feedback" description="Review submitted ratings and comments from BananaQ." />
    <RecordTable view={view} columns={columns.length ? columns : safeFallback}
      searchFields={searchFields} facets={facets} />
  </>
}

export function ScanPage({ view }: { view: View }) {
  const columns = [
    ...labelField(view.date, 'Scanned at'),
    ...labelField(view.label, 'Predicted label'),
    ...labelField(view.confidence, 'Confidence'),
    ...labelField(field(view, 'is_banana_leaf'), 'Banana leaf'),
    ...labelField(field(view, 'scan_id') ?? view.fields.find((column) => column.primaryKey), 'Scan ID'),
    ...labelField(field(view, 'participant_id'), 'Participant ID'),
    ...labelField(field(view, 'session_id'), 'Session ID'),
  ]
  return <>
    <PageHeading title="Scan Records" description="Inspect scan timestamps, predictions, and available confidence scores." />
    <RecordTable view={view} columns={columns} searchFields={searchable(view)}
      facets={view.label ? [{ field: view.label, label: 'Predicted label' }] : []} />
  </>
}

function RelatedSessions({ sessions, relationship, row }: { sessions: Table; relationship: Relationship; row: Row }) {
  const [page, setPage] = useState(0)
  const size = 10
  const params = useMemo(() => Object.fromEntries(relationship.columns.map((name, index) =>
    [name, String(row.record[relationship.targetColumns[index]] ?? '')])), [relationship, row])
  const load = useCallback((signal: AbortSignal) => getData<Page>({ table: sessions },
    { p_filters: params, p_limit: size, p_offset: page * size }, signal), [sessions, params, page])
  const { data, loading, error } = useRequest(load)
  const columns = toFields(sessions).filter((column) =>
    column.primaryKey || ['started_at', 'last_active_at', 'ended_at'].includes(column.name))
  return <section>
    <h3>Related sessions</h3>
    {loading ? <Loading /> : error ? <ErrorState error={error} /> : data?.rows.length ? <>
      <div className="table-scroll"><table>
        <thead><tr>{columns.map((column) => <th key={column.key} scope="col">{column.label}</th>)}</tr></thead>
        <tbody>{data.rows.map((session, index) => <tr key={index}>
          {columns.map((column) => <td key={column.key}>{formatCell(valueOf(session, column), column)}</td>)}
        </tr>)}</tbody>
      </table></div>
      <div className="pagination"><span>{countFormat.format(data.total)} related sessions</span>
        <div><button type="button" className="button secondary icon-only" aria-label="Previous sessions"
          disabled={!page} onClick={() => setPage(page - 1)}><ChevronLeft size={17} /></button>
          <button type="button" className="button secondary icon-only" aria-label="More sessions"
            disabled={(page + 1) * size >= data.total} onClick={() => setPage(page + 1)}><ChevronRight size={17} /></button></div>
      </div>
    </> : <Empty label="No related sessions." />}
  </section>
}

export function ParticipantPage({ table, sessions }: { table: Table; sessions?: Table }) {
  const view = useMemo(() => ({ table, fields: toFields(table) }), [table])
  const relationship = sessions?.relationships.find((key) =>
    key.readable && key.targetSchema === 'public' && key.targetTable === table.name)
  return <>
    <PageHeading title="Participants" description={relationship
      ? 'Review participant records and sessions linked by the database.'
      : 'Review participant records. No verified session relationship is available.'} />
    <RecordTable view={view} columns={view.fields.slice(0, 6)} searchFields={searchable(view)} exactFilter
      extraDetails={relationship && sessions ? (row) => relationship.targetColumns.every((name) => row.record[name] != null)
        ? <RelatedSessions sessions={sessions} relationship={relationship} row={row} /> : null : undefined} />
  </>
}
