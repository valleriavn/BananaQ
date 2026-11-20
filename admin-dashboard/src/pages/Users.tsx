import { useCallback, useEffect, useRef, useState } from 'react'
import { ChevronLeft, ChevronRight, Eye, Trash2, X } from 'lucide-react'
import type { OwnerSummary } from '../lib/data'
import { Empty, ErrorState, Loading, PageHeading, RefreshButton } from '../components/Ui'
import { useRequest } from '../hooks/useRequest'
import { deleteOwnerRecords, errorMessage, getUsers } from '../lib/data'
import { countFormat } from '../lib/format'
import { humanize } from '../lib/schema'

const PAGE_SIZE = 100

function OwnerDetails({ owner, onClose, onDelete }: { owner: OwnerSummary; onClose: () => void; onDelete: () => Promise<void> }) {
  const dialog = useRef<HTMLDialogElement>(null)
  const [deleting, setDeleting] = useState(false)
  const [deleteError, setDeleteError] = useState('')
  useEffect(() => {
    const element = dialog.current
    element?.showModal()
    return () => element?.close()
  }, [])
  return <dialog ref={dialog} className="detail-dialog" onCancel={(event) => { event.preventDefault(); onClose() }}
    onClick={(event) => { if (event.target === event.currentTarget) onClose() }} aria-labelledby="owner-detail-title">
    <div className="detail-body">
      <header><div><h2 id="owner-detail-title">User details</h2><p className="muted">Study owner summary</p></div>
        <button className="icon-button" type="button" onClick={onClose} aria-label="Close details"><X size={22} /></button>
      </header>
      <section>
        <h3>Owner information</h3>
        <dl className="detail-fields">
          <div><dt>Owner ID</dt><dd>{owner.owner_id}</dd></div>
          <div><dt>Study records</dt><dd>{countFormat.format(owner.record_count)}</dd></div>
        </dl>
      </section>
      <section>
        <h3>Records by table</h3>
        {owner.sources.length ? <div className="table-scroll"><table>
          <thead><tr><th scope="col">Table</th><th scope="col">Records</th></tr></thead>
          <tbody>{owner.sources.map((source) => <tr key={source.table}>
            <td>{source.table}</td><td>{countFormat.format(source.records)}</td>
          </tr>)}</tbody>
        </table></div> : <Empty label="No related study records found." />}
      </section>
      <section className="record-actions">
        <h3>Actions</h3>
        <p className="muted">Permanently deletes this owner’s study records from the dashboard tables. The Auth account is not deleted.</p>
        {deleteError && <p className="problem-text" role="alert">{deleteError}</p>}
        <button className="button danger" type="button" disabled={deleting} onClick={async () => {
          if (!window.confirm(`Permanently delete ${countFormat.format(owner.record_count)} study records for owner ${owner.owner_id}? This cannot be undone.`)) return
          setDeleting(true)
          setDeleteError('')
          try { await onDelete() } catch (error) { setDeleteError(errorMessage(error)); setDeleting(false) }
        }}>{deleting ? 'Deleting…' : <><Trash2 size={16} /> Delete study records</>}</button>
      </section>
    </div>
  </dialog>
}

export function UsersPage() {
  const [page, setPage] = useState(0)
  const [revision, setRevision] = useState(0)
  const [selectedOwner, setSelectedOwner] = useState<OwnerSummary | null>(null)
  const [notice, setNotice] = useState('')
  const request = useCallback((signal: AbortSignal) => getUsers(page * PAGE_SIZE, signal), [page])
  const { data, loading, error } = useRequest(request, revision)
  return <div className="records-stack">
    <PageHeading title="Users" 
      actions={<RefreshButton onClick={() => setRevision((value) => value + 1)} loading={loading} />} />
    {notice && <p className="success-message" role="status">{notice}</p>}
    <section className="panel records-panel" aria-label="User results" aria-busy={loading}>
      {loading ? <Loading label="Loading users…" /> : error ? <ErrorState error={error} retry={() => setRevision((value) => value + 1)} />
        : data?.rows.length ? <div className="table-scroll"><table>
          <thead><tr><th scope="col">Owner ID</th><th scope="col">Study records</th><th scope="col">Activity summary</th><th scope="col">Details</th></tr></thead>
          <tbody>{data.rows.map((owner) => <tr key={owner.owner_id}>
            <td><span className="cell-value" title={owner.owner_id}>{owner.owner_id}</span></td>
            <td>{countFormat.format(owner.record_count)}</td>
            <td>{owner.sources.length ? <span className="activity-summary">{owner.sources.map((source) =>
              `${humanize(source.table)}: ${countFormat.format(source.records)}`).join(' · ')}</span> : 'No activity'}</td>
            <td><button className="button secondary table-detail" type="button" onClick={() => setSelectedOwner(owner)}
              aria-label={`View details for owner ${owner.owner_id}`}><Eye size={16} /> View</button></td>
          </tr>)}</tbody>
        </table></div> : <Empty label="No owner IDs found in approved study records." />}
      {data && <footer className="pagination">
        <span role="status">Showing {countFormat.format(data.rows.length ? page * PAGE_SIZE + 1 : 0)}-{countFormat.format(page * PAGE_SIZE + data.rows.length)} of {countFormat.format(data.total)}</span>
        <div>
          <button type="button" className="button secondary icon-only" aria-label="Previous users" disabled={page === 0 || loading}
            onClick={() => setPage((value) => value - 1)}><ChevronLeft size={18} /></button>
          <span className="page-number" aria-label={`Page ${page + 1}`}>{page + 1}</span>
          <button type="button" className="button secondary icon-only" aria-label="Next users" disabled={loading || (page + 1) * PAGE_SIZE >= data.total}
            onClick={() => setPage((value) => value + 1)}><ChevronRight size={18} /></button>
        </div>
      </footer>}
    </section>
    {selectedOwner && <OwnerDetails owner={selectedOwner} onClose={() => setSelectedOwner(null)} onDelete={async () => {
      const controller = new AbortController()
      await deleteOwnerRecords(selectedOwner.owner_id, controller.signal)
      setSelectedOwner(null)
      setNotice(`Study records for owner ${selectedOwner.owner_id} were deleted.`)
      setRevision((value) => value + 1)
    }} />}
  </div>
}
