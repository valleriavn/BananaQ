import { Database, KeyRound, Link2 } from 'lucide-react'
import { useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import { RecordTable } from '../components/RecordTable'
import { Empty, PageHeading, RefreshButton } from '../components/Ui'
import { isDate, toFields } from '../lib/schema'
import type { Table } from '../lib/schema'

function TableDetails({ table }: { table: Table }) {
  const view = useMemo(() => {
    const fields = toFields(table)
    return { table, fields, date: fields.find(isDate) }
  }, [table])
  const searchFields = view.fields.filter((field) => /^(text|character|citext|uuid)/.test(field.type))
  return <div className="explorer-detail">
    <section className="panel schema-panel">
      <h2><Database size={21} /> {table.name}</h2>
      <div className="table-scroll">
        <table>
          <thead><tr><th scope="col">Column</th><th scope="col">Data type</th><th scope="col">Nullable</th><th scope="col">Key</th></tr></thead>
          <tbody>{table.columns.map((column) => <tr key={column.name}>
            <td><code>{column.name}</code></td><td>{column.type}</td>
            <td>{column.nullable ? 'Yes' : 'No'}</td>
            <td>{column.primaryKey ? <span className="inline-icon"><KeyRound size={15} /> Primary</span> : '—'}</td>
          </tr>)}</tbody>
        </table>
      </div>
      <div className="relationships">
        <h3><Link2 size={17} /> Foreign keys</h3>
        {table.relationships.length ? <ul>{table.relationships.map((key) => <li key={key.name}>
          <code>{key.columns.join(', ')}</code><span aria-hidden="true"> → </span>
          <code>{key.targetSchema}.{key.targetTable} ({key.targetColumns.join(', ')})</code>
          {!key.readable && <small>Metadata only</small>}
        </li>)}</ul> : <p>No foreign keys discovered.</p>}
      </div>
    </section>
    <h2 className="section-heading">Table records</h2>
    <RecordTable view={view} columns={view.fields.slice(0, 7)} searchFields={searchFields} exactFilter />
  </div>
}

export function Explorer({ tables, refreshSchema }: { tables: Table[]; refreshSchema: () => void }) {
  const [params, setParams] = useSearchParams()
  const active = tables.find((table) => table.name === params.get('table')) ?? tables[0]
  return <>
    <PageHeading title="Database Explorer" description="Inspect approved Supabase tables, columns, relationships, and records."
      actions={<RefreshButton onClick={refreshSchema} />} />
    {!active ? <section className="panel"><Empty label="No approved tables discovered."
      detail="Ask the project owner to approve the study tables for dashboard access." /></section>
      : <div className="explorer-grid">
        <section className="panel table-picker" aria-label="Discovered tables">
          <h2>Tables <small>{tables.length}</small></h2>
          {tables.map((table) => <button className={active.name === table.name ? 'selected' : undefined} type="button"
            aria-pressed={active.name === table.name} key={table.name}
            onClick={() => setParams({ table: table.name })}><Database size={17} /><span>{table.name}</span></button>)}
        </section>
        <TableDetails key={active.name} table={active} />
      </div>}
  </>
}
