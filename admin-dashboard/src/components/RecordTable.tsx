import { ArrowDownWideNarrow, ArrowUpNarrowWide, ChevronLeft, ChevronRight, Eye, Search, X } from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import { useRequest } from '../hooks/useRequest'
import { getData, getSeries, valueOf } from '../lib/data'
import type { Page, Row } from '../lib/data'
import { countFormat, formatCell } from '../lib/format'
import type { Field, View } from '../lib/schema'
import { Empty, ErrorState, Loading, RefreshButton } from './Ui'

type Filters = Record<string, string>
export type Facet = { field: Field; label: string }

function FacetSelect({ view, facet, value, onChange }: {
  view: View; facet: Facet; value: string; onChange: (value: string) => void
}) {
  const { field, label } = facet
  const request = useCallback((signal: AbortSignal) =>
    getSeries(view, field, 'distribution', signal, { limit: 100 }), [view, field])
  const { data, error, loading } = useRequest(request)
  return <label>{label}
    <select aria-label={label} value={value} onChange={(event) => onChange(event.target.value)} disabled={loading || !!error}>
      <option value="">{loading ? 'Loading…' : error ? 'Unavailable' : 'All'}</option>
      {data?.filter((item) => item.label !== '').map(({ label: option }) =>
        <option value={option} key={option}>{field.type === 'boolean' ? (option === 'true' ? 'Yes' : 'No') : option}</option>
      )}
    </select>
    {error ? <small>Could not load filter options.</small> : data?.length === 100 ? <small>Top 100 values</small> : null}
  </label>
}

function Details({ row, view, onClose, extra }: {
  row: Row; view: View; onClose: () => void; extra?: (row: Row) => ReactNode
}) {
  const dialog = useRef<HTMLDialogElement>(null)
  useEffect(() => {
    const element = dialog.current
    element?.showModal()
    return () => element?.close()
  }, [])
  const groups = [
    { label: 'Record fields', fields: view.fields.filter((field) => !field.related) },
    { label: 'Related record fields', fields: view.fields.filter((field) => field.related) },
  ]
  return <dialog ref={dialog} className="detail-dialog" onCancel={(event) => { event.preventDefault(); onClose() }}
    aria-labelledby="detail-title" onClick={(event) => { if (event.target === event.currentTarget) onClose() }}>
    <div className="detail-body">
      <header><div><h2 id="detail-title">Record details</h2><p className="muted">{view.table.name}</p></div>
        <button className="icon-button" type="button" onClick={onClose} aria-label="Close details"><X size={22} /></button>
      </header>
      {groups.filter((group) => group.fields.length).map((group) =>
        <section key={group.label}>
          <h3>{group.label}</h3>
          <dl className="detail-fields">{group.fields.map((field) => <div key={field.key}>
            <dt>{field.label}</dt><dd>{formatCell(valueOf(row, field), field)}</dd>
          </div>)}</dl>
        </section>
      )}
      {extra?.(row)}
    </div>
  </dialog>
}

type RecordTableProps = {
  view: View; columns: Field[]; searchFields: Field[]; facets?: Facet[];
  exactFilter?: boolean; extraDetails?: (row: Row) => ReactNode;
}

export function RecordTable({ view, columns, searchFields, facets = [], exactFilter, extraDetails }: RecordTableProps) {
  const [draft, setDraft] = useState('')
  const [search, setSearch] = useState('')
  const [filters, setFilters] = useState<Filters>({})
  const [filterField, setFilterField] = useState('')
  const [filterValue, setFilterValue] = useState('')
  const [sort, setSort] = useState(view.date?.key ?? '')
  const [descending, setDescending] = useState(Boolean(view.date))
  const [size, setSize] = useState(25)
  const [page, setPage] = useState(0)
  const [revision, setRevision] = useState(0)
  const [detail, setDetail] = useState<Row | null>(null)
  const fieldKeys = searchFields.slice(0, 24).map((field) => field.key).join('\u0000')
  const load = useCallback((signal: AbortSignal) => getData<Page>(view, {
    p_search: search,
    p_search_fields: fieldKeys ? fieldKeys.split('\u0000') : [],
    p_filters: filters,
    p_sort: sort || null,
    p_desc: descending,
    p_limit: size,
    p_offset: page * size,
  }, signal), [view, search, fieldKeys, filters, sort, descending, size, page])
  const { data, loading, error } = useRequest(load, revision)
  const refresh = () => setRevision((value) => value + 1)
  const changeFacet = (key: string, value: string) => {
    setFilters((previous) => {
      const next = { ...previous }
      if (value) next[key] = value
      else delete next[key]
      return next
    })
    setPage(0)
  }
  const reset = () => {
    setDraft(''); setSearch(''); setFilters({}); setFilterField(''); setFilterValue(''); setPage(0)
  }
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setPage(0)
    setSearch(draft.trim())
    if (exactFilter) setFilters(filterField && filterValue !== '' ? { [filterField]: filterValue } : {})
  }

  const total = data?.total ?? 0
  const first = data?.rows.length ? page * size + 1 : 0
  const last = data?.rows.length ? page * size + data.rows.length : 0
  const displayColumns = columns
  const sortFields = view.fields.filter((field) => /^(text|character|citext|uuid|boolean|real|double precision|numeric|decimal|smallint|integer|bigint|date|timestamp)\b/.test(field.type))
  const canReset = !!(draft || search || filterField || Object.keys(filters).length)

  return <div className="records-stack">
    <form className="panel table-controls" onSubmit={submit}>
      {searchFields.length > 0 && <label className="search-control">Search
        <span className="icon-input"><Search size={17} /><input value={draft} onChange={(event) => setDraft(event.target.value)}
          placeholder="Search records…" maxLength={200} /></span>
      </label>}
      {facets.map((facet) => <FacetSelect key={facet.field.key} facet={facet} view={view}
        value={filters[facet.field.key] ?? ''} onChange={(value) => changeFacet(facet.field.key, value)} />)}
      {exactFilter && <>
        <label>Filter column<select value={filterField} onChange={(event) => setFilterField(event.target.value)}>
          <option value="">All records</option>
          {sortFields.map((field) => <option value={field.key} key={field.key}>{field.label}</option>)}
        </select></label>
        <label>Exact value<input value={filterValue} onChange={(event) => setFilterValue(event.target.value)}
          disabled={!filterField} placeholder="Enter a value" maxLength={200} /></label>
      </>}
      <button className="button primary" type="submit">Apply filters</button>
      <button className="button secondary" type="button" onClick={reset} disabled={!canReset}>Reset</button>
    </form>
    <div className="table-toolbar">
      <div className="sort-controls">
        <label>Sort by<select value={sort} onChange={(event) => { setSort(event.target.value); setPage(0) }}>
          <option value="">Default order</option>
          {sortFields.map((field) => <option value={field.key} key={field.key}>{field.label}</option>)}
        </select></label>
        <button className="button secondary icon-only" type="button" title={descending ? 'Newest or highest first' : 'Oldest or lowest first'}
          aria-label={descending ? 'Switch to ascending order' : 'Switch to descending order'} disabled={!sort}
          onClick={() => { setDescending(!descending); setPage(0) }}>
          {descending ? <ArrowDownWideNarrow size={18} /> : <ArrowUpNarrowWide size={18} />}
        </button>
      </div>
      <RefreshButton onClick={refresh} loading={loading} />
    </div>
    <section className="panel records-panel" aria-label="Record results" aria-busy={loading}>
      {loading ? <Loading /> : error ? <ErrorState error={error} retry={refresh} /> : <div className="table-scroll">
        <table>
          <thead><tr>{displayColumns.map((field) => <th scope="col" key={field.key}>{field.label}</th>)}<th scope="col">Details</th></tr></thead>
          <tbody>{data?.rows.length ? data.rows.map((row, index) => {
            const primary = view.table.columns.filter((column) => column.primaryKey).map((column) => row.record[column.name])
            return <tr key={primary.length ? JSON.stringify(primary) : index}>
              {displayColumns.map((field) => {
                const text = formatCell(valueOf(row, field), field)
                return <td key={field.key}><span className="cell-value" title={text.length > 50 ? text : undefined}>{text}</span></td>
              })}
              <td><button className="button secondary table-detail" type="button" onClick={() => setDetail(row)}
                aria-label={'View details for row ' + (page * size + index + 1)}><Eye size={16} /> View</button></td>
            </tr>
          }) : <tr><td colSpan={displayColumns.length + 1}>
            <Empty label={search || Object.keys(filters).length ? 'No records match these filters.' : 'No records available.'}
              detail={page ? 'Go to a previous page to see available records.' : undefined} />
          </td></tr>}</tbody>
        </table>
      </div>}
      <footer className="pagination">
        <span role="status">{data ? `Showing ${countFormat.format(first)}–${countFormat.format(last)} of ${countFormat.format(total)}` : 'Records'}</span>
        <div>
          <label>Rows<select value={size} onChange={(event) => { setSize(Number(event.target.value)); setPage(0) }}>
            <option value={10}>10</option><option value={25}>25</option><option value={50}>50</option>
          </select></label>
          <button type="button" className="button secondary icon-only" aria-label="Previous page" disabled={page === 0 || loading}
            onClick={() => setPage((current) => current - 1)}><ChevronLeft size={18} /></button>
          <span className="page-number" aria-label={`Page ${page + 1}`}>{page + 1}</span>
          <button type="button" className="button secondary icon-only" aria-label="Next page" disabled={loading || (page + 1) * size >= total}
            onClick={() => setPage((current) => current + 1)}><ChevronRight size={18} /></button>
        </div>
      </footer>
    </section>
    {detail && <Details row={detail} view={view} onClose={() => setDetail(null)} extra={extraDetails} />}
  </div>
}
