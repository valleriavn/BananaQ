import { Download, ShieldCheck, Upload } from 'lucide-react'
import { useRef, useState } from 'react'
import type { ChangeEvent } from 'react'
import { SignOut } from '../components/Auth'
import { Loading, PageHeading } from '../components/Ui'
import { errorMessage, getData, restoreTable } from '../lib/data'
import type { Page } from '../lib/data'
import { projectHost } from '../lib/supabase'
import type { Table } from '../lib/schema'

const BACKUP_FORMAT = 'bananaq-admin-backup'
const MAX_FILE_BYTES = 50 * 1024 * 1024
const MAX_ROWS_PER_TABLE = 50_000

function dependencyOrder(tables: Table[]) {
  const byName = new Map(tables.map((table) => [table.name, table]))
  const visiting = new Set<string>()
  const visited = new Set<string>()
  const ordered: Table[] = []
  function visit(table: Table) {
    if (visited.has(table.name) || visiting.has(table.name)) return
    visiting.add(table.name)
    table.relationships.filter((relation) => relation.readable && relation.targetSchema === 'public')
      .map((relation) => byName.get(relation.targetTable)).filter((target): target is Table => !!target)
      .forEach(visit)
    visiting.delete(table.name)
    visited.add(table.name)
    ordered.push(table)
  }
  tables.forEach(visit)
  return ordered
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function DataRecovery({ tables }: { tables: Table[] }) {
  const input = useRef<HTMLInputElement>(null)
  const [busy, setBusy] = useState<'backup' | 'restore' | null>(null)
  const [message, setMessage] = useState('')
  const [error, setError] = useState<unknown>()

  async function createBackup() {
    setBusy('backup'); setError(undefined); setMessage('')
    try {
      const exportTables = []
      for (const table of dependencyOrder(tables)) {
        const rows: Record<string, unknown>[] = []
        let offset = 0
        let total = 0
        do {
          const controller = new AbortController()
          const page = await getData<Page>({ table }, { p_mode: 'records', p_limit: 100, p_offset: offset }, controller.signal)
          total = page.total
          rows.push(...page.rows.map((row) => row.record))
          offset += page.rows.length
          if (rows.length > MAX_ROWS_PER_TABLE) throw new Error(`${table.name} exceeds the ${MAX_ROWS_PER_TABLE.toLocaleString()} row backup limit.`)
        } while (offset < total)
        exportTables.push({ name: table.name, rows })
      }
      const backup = { format: BACKUP_FORMAT, version: 1, createdAt: new Date().toISOString(), tables: exportTables }
      const content = JSON.stringify(backup, null, 2)
      if (new Blob([content]).size > MAX_FILE_BYTES) throw new Error('Backup is larger than the 50 MB download limit.')
      const url = URL.createObjectURL(new Blob([content], { type: 'application/json' }))
      const link = document.createElement('a')
      link.href = url
      link.download = `bananaq-backup-${new Date().toISOString().slice(0, 10)}.json`
      link.click()
      window.setTimeout(() => URL.revokeObjectURL(url), 1000)
      setMessage(`Backup downloaded with ${exportTables.reduce((sum, table) => sum + table.rows.length, 0).toLocaleString()} records across ${exportTables.length} tables.`)
    } catch (reason) { setError(reason) } finally { setBusy(null) }
  }

  async function restoreBackup(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    setBusy('restore'); setError(undefined); setMessage('')
    let restored = 0
    try {
      if (file.size > MAX_FILE_BYTES) throw new Error('Choose a backup file smaller than 50 MB.')
      const parsed: unknown = JSON.parse(await file.text())
      if (!isObject(parsed) || parsed.format !== BACKUP_FORMAT || parsed.version !== 1 || !Array.isArray(parsed.tables)) {
        throw new Error('This file is not a supported BananaQ dashboard backup.')
      }
      const approved = new Map(tables.map((table) => [table.name, table]))
      const imported = parsed.tables as unknown[]
      const backupTables = imported.map((item) => {
        if (!isObject(item) || typeof item.name !== 'string' || !Array.isArray(item.rows)) {
          throw new Error('The backup contains an invalid table entry.')
        }
        if (!approved.has(item.name)) throw new Error(`The table “${item.name}” is not approved in this project.`)
        if (item.rows.length > MAX_ROWS_PER_TABLE || !item.rows.every(isObject)) {
          throw new Error(`The backup has invalid or too many records for ${item.name}.`)
        }
        return { name: item.name, rows: item.rows as Record<string, unknown>[] }
      })
      if (new Set(backupTables.map((table) => table.name)).size !== backupTables.length) {
        throw new Error('The backup contains a duplicate table entry.')
      }
      if (!window.confirm('Restore this backup? Matching primary keys will be updated and missing records inserted. Existing records absent from the backup will be kept. This cannot be rolled back.')) return

      const ordered = dependencyOrder(backupTables.map((backupTable) => approved.get(backupTable.name)!))
      for (const table of ordered) {
        const rows = backupTables.find((item) => item.name === table.name)?.rows ?? []
        for (let offset = 0; offset < rows.length; offset += 250) {
          const controller = new AbortController()
          restored += await restoreTable(table.name, rows.slice(offset, offset + 250), controller.signal)
        }
      }
      setMessage(`Restore complete: ${restored.toLocaleString()} records inserted or updated across ${backupTables.length} tables. Records not in the backup were kept.`)
    } catch (reason) {
      const reasonText = reason instanceof Error ? reason.message : errorMessage(reason)
      setError(restored ? new Error(`Restore stopped after ${restored.toLocaleString()} records had already been applied. ${reasonText}`) : reason)
    } finally { setBusy(null) }
  }

  return <section className="panel settings-card recovery-card">
    <h2>Backup and restore</h2>
    <div className="recovery-actions">
      <button type="button" className="button primary" disabled={!!busy || !tables.length} onClick={() => void createBackup()}>
        <Download size={17} /> {busy === 'backup' ? 'Preparing backup…' : 'Download backup'}
      </button>
      <button type="button" className="button secondary" disabled={!!busy || !tables.length} onClick={() => input.current?.click()}>
        <Upload size={17} /> {busy === 'restore' ? 'Restoring…' : 'Restore from file'}
      </button>
      <input ref={input} className="visually-hidden" type="file" accept="application/json,.json" onChange={(event) => void restoreBackup(event)} />
    </div>
    {busy && <Loading label={busy === 'backup' ? 'Reading approved tables…' : 'Restoring backup records…'} />}
    {error !== undefined ? <p className="problem-text" role="alert">{error instanceof Error ? error.message : errorMessage(error)}</p> : null}
    {message && <p className="success-message" role="status">{message}</p>}
  </section>
}

export function SettingsPage({ email, tables }: { email: string; tables: Table[] }) {
  return <>
    <PageHeading title="Settings" />
    <div className="settings-grid">
      <section className="panel settings-card">
        <h2><ShieldCheck size={21} /> Administrator account</h2>
        <dl><dt>Signed in as</dt><dd>{email}</dd></dl>
        <SignOut />
      </section>
      <section className="panel settings-card">
        <h2>Supabase project</h2>
        <dl><dd className="break-text">{projectHost}</dd></dl>
      </section>
      <DataRecovery tables={tables} />
    </div>
  </>
}
