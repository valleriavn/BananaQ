import { BarChart3, Database, MessageCircle, ScanLine, UsersRound } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { useCallback, useState } from 'react'
import { Area, AreaChart, Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { Link } from 'react-router-dom'
import { Empty, ErrorState, Loading, PageHeading, RefreshButton, Status } from '../components/Ui'
import { useRequest } from '../hooks/useRequest'
import { getSeries, getTotal } from '../lib/data'
import { countFormat } from '../lib/format'
import type { Field, StudyViews, Table, View } from '../lib/schema'
import { projectHost } from '../lib/supabase'

function Total({ table, title, Icon, revision }: { table: Table; title: string; Icon: LucideIcon; revision: number }) {
  const request = useCallback((signal: AbortSignal) => getTotal(table, signal), [table])
  const { data, loading, error } = useRequest(request, revision)
  return <section className="panel metric-card">
    <div className="metric-icon"><Icon size={26} /></div>
    <div><h2>{title}</h2><strong aria-live="polite">{data === undefined ? '—' : countFormat.format(data)}</strong>
      {loading ? <small>Loading total…</small> : error ? <small>Count unavailable</small> : null}</div>
  </section>
}

const shortDate = new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric', timeZone: 'UTC' })
const chartDate = (label: string) => shortDate.format(new Date(label + 'T00:00:00Z'))
const resultLabel = (value: string) => value === 'true' ? 'Yes' : value === 'false' ? 'No' : value

type MetricProps = {
  view: View; field: Field; title: string; revision: number;
  mode: 'daily' | 'distribution' | 'confidence'; days?: number; limit?: number; description?: string;
}

function MetricChart({ view, field, mode, title, revision, days = 30, limit = 12, description }: MetricProps) {
  const [attempt, setAttempt] = useState(0)
  const request = useCallback((signal: AbortSignal) =>
    getSeries(view, field, mode, signal, { days, limit }), [view, field, mode, days, limit])
  const { data, loading, error } = useRequest(request, revision + attempt)
  const daily = mode === 'daily'
  const show = data?.some((point) => point.count > 0)
  const label = daily ? chartDate : field.type === 'boolean' ? resultLabel : (value: string) => value
  const points = data?.map((point) => ({ ...point, display: label(point.label) })) ?? []
  return <section className="panel chart-panel">
    <h2>{title}</h2>
    {description && <p className="chart-caption">{description}</p>}
    {loading ? <Loading /> : error ? <ErrorState error={error} retry={() => setAttempt((value) => value + 1)} />
      : !show ? <Empty label={daily ? 'No records in this period.' : 'No records available.'} />
      : <>
        <div className="chart" aria-hidden="true">
          <ResponsiveContainer width="100%" height="100%">
            {daily ? <AreaChart data={points} accessibilityLayer={false} margin={{ top: 12, right: 12, bottom: 0, left: 0 }}>
              <CartesianGrid vertical={false} stroke="var(--color-divider)" />
              <XAxis dataKey="display" tick={{ fontSize: 11, fill: 'var(--color-text-secondary)' }} minTickGap={22} />
              <YAxis width={40} allowDecimals={false} tick={{ fontSize: 11, fill: 'var(--color-text-secondary)' }} />
              <Tooltip formatter={(value) => [String(value), 'Records']} />
              <Area dataKey="count" type="monotone" fill="var(--color-accent-light)"
                stroke="var(--color-heading)" strokeWidth={2} isAnimationActive={false} />
            </AreaChart> : <BarChart data={points} accessibilityLayer={false} layout="vertical" margin={{ top: 6, right: 12, bottom: 0, left: 6 }}>
              <CartesianGrid horizontal={false} stroke="var(--color-divider)" />
              <XAxis type="number" allowDecimals={false} tick={{ fontSize: 11, fill: 'var(--color-text-secondary)' }} />
              <YAxis type="category" dataKey="display" width={105} tick={{ fontSize: 11, fill: 'var(--color-text-secondary)' }} />
              <Tooltip formatter={(value) => [String(value), 'Records']} />
              <Bar dataKey="count" fill="var(--color-primary)" radius={[0, 5, 5, 0]} maxBarSize={26} isAnimationActive={false} />
            </BarChart>}
          </ResponsiveContainer>
        </div>
        <details className="chart-values">
          <summary>View chart data</summary>
          <div className="table-scroll"><table><thead><tr><th scope="col">{daily ? 'UTC date' : 'Category'}</th><th scope="col">Records</th></tr></thead>
            <tbody>{points.map((point) => <tr key={point.label}><td>{daily ? point.label : point.display}</td>
              <td>{countFormat.format(point.count)}</td></tr>)}</tbody>
          </table></div>
        </details>
      </>}
  </section>
}

export function Overview({ views, tables }: { views: StudyViews; tables: Table[] }) {
  const [revision, setRevision] = useState(0)
  const { scans, feedback, participants, sessions } = views
  return <>
    <PageHeading title="Dashboard Overview" description="Monitor BananaQ activity, data, and system status."
      actions={<RefreshButton onClick={() => setRevision((value) => value + 1)} />} />
    {!tables.length && <section className="panel"><Empty label="No approved tables discovered." /></section>}
    <div className="metrics-grid">
      {scans && <Total table={scans.table} title="Scans" Icon={ScanLine} revision={revision} />}
      {feedback && <Total table={feedback.table} title="Feedback submissions" Icon={MessageCircle} revision={revision} />}
      {sessions && <Total table={sessions} title="Sessions" Icon={UsersRound} revision={revision} />}
      {participants && <Total table={participants} title="Participants" Icon={UsersRound} revision={revision} />}
    </div>
    {(scans?.date || feedback?.date) && <div className="chart-grid">
      {scans?.date && <MetricChart view={scans} field={scans.date} mode="daily" title="Scans over time"
        description="Last 30 days, grouped by UTC date" revision={revision} />}
      {feedback?.date && <MetricChart view={feedback} field={feedback.date} mode="daily" title="Feedback over time"
        description="Last 30 days, grouped by UTC date" revision={revision} />}
    </div>}
    <section className="panel source-panel">
      <div><h2>Source status</h2><p><Database size={20} /> {tables.length} approved {tables.length === 1 ? 'table' : 'tables'} in Supabase</p></div>
      <div className="source-actions"><Status label="Schema verified" /><Link to="/database" className="button secondary">Explore tables</Link></div>
      <small className="break-text">{projectHost}</small>
      {!participants && <small>No participant table was discovered. Sessions and installation IDs are separate from participant counts.</small>}
    </section>
    {(scans?.label || feedback?.rating || feedback?.result) && <div className="chart-grid">
      {scans?.label && <MetricChart view={scans} field={scans.label} mode="distribution"
        title="Most frequent scan predictions" description="Top 12 labels across recorded scans" revision={revision} />}
      {feedback?.rating && <MetricChart view={feedback} field={feedback.rating} mode="distribution"
        title="Feedback ratings" description="Rating counts among submitted feedback records" revision={revision} />}
      {feedback?.result && <MetricChart view={feedback} field={feedback.result} mode="distribution"
        title="Prediction validity among feedback" revision={revision} />}
    </div>}
  </>
}

export function Analytics({ views }: { views: StudyViews }) {
  const [revision, setRevision] = useState(0)
  const [days, setDays] = useState(30)
  const { scans, feedback } = views
  const hasMetrics = scans?.date || scans?.label || scans?.confidence || scans?.result ||
    feedback?.date || feedback?.label || feedback?.rating || feedback?.result || feedback?.confidence
  return <>
    <PageHeading title="Analytics" description="Track study activity, prediction patterns, and ratings."
      actions={<RefreshButton onClick={() => setRevision((value) => value + 1)} />} />
    {!hasMetrics ? <section className="panel"><Empty label="No supported analytics columns were discovered." /></section> : <>
      {(scans?.date || feedback?.date) && <>
        <div className="analytics-period">
          <h2><BarChart3 size={21} /> Trends</h2>
          <label>Time series period<select value={days} onChange={(event) => setDays(Number(event.target.value))}>
            <option value={30}>Last 30 days</option><option value={90}>Last 90 days</option><option value={365}>Last 365 days</option>
          </select></label>
        </div>
        <div className="chart-grid">
          {scans?.date && <MetricChart view={scans} field={scans.date} mode="daily" days={days} title="Scans over time"
            description="Scans grouped by UTC date" revision={revision} />}
          {feedback?.date && <MetricChart view={feedback} field={feedback.date} mode="daily" days={days} title="Feedback over time"
            description="Submissions grouped by UTC date" revision={revision} />}
        </div>
      </>}
      <h2 className="section-heading">Distributions</h2>
      <div className="chart-grid">
        {scans?.label && <MetricChart view={scans} field={scans.label} mode="distribution"
          title="Labels across scans" description="Top 12 non-empty prediction labels" revision={revision} />}
        {feedback?.label && <MetricChart view={feedback} field={feedback.label} mode="distribution"
          title="Labels among feedback submissions" description="Top 12 labels on feedback with a prediction" revision={revision} />}
        {feedback?.rating && <MetricChart view={feedback} field={feedback.rating} mode="distribution"
          title="Feedback ratings" description="Recorded accuracy ratings" revision={revision} />}
        {feedback?.result && <MetricChart view={feedback} field={feedback.result} mode="distribution"
          title="Prediction validity among feedback" description="Uses the recorded validity field" revision={revision} />}
        {scans?.result && <MetricChart view={scans} field={scans.result} mode="distribution"
          title="Prediction validity across scans" description="Uses the recorded validity field" revision={revision} />}
        {scans?.confidence && <MetricChart view={scans} field={scans.confidence} mode="confidence"
          title="Scan confidence distribution" description="Scans with recorded confidence, in 20 percentage-point bands" revision={revision} />}
        {feedback?.confidence && <MetricChart view={feedback} field={feedback.confidence} mode="confidence"
          title="Confidence among feedback submissions" description="Feedback with recorded prediction confidence" revision={revision} />}
      </div>
    </>}
  </>
}
