import { lazy, Suspense, useMemo, useState } from 'react'
import { HashRouter, Navigate, Route, Routes } from 'react-router-dom'
import type { Session } from '@supabase/supabase-js'
import { AuthFrame, Login, SignOut, useSession } from './components/Auth'
import { Layout } from './components/Layout'
import { ErrorState, Loading, PageHeading } from './components/Ui'
import { useRequest } from './hooks/useRequest'
import { DashboardError, getSchema } from './lib/data'
import { studyViews } from './lib/schema'

const Overview = lazy(() => import('./pages/Metrics').then((module) => ({ default: module.Overview })))
const Analytics = lazy(() => import('./pages/Metrics').then((module) => ({ default: module.Analytics })))
const Explorer = lazy(() => import('./pages/Explorer').then((module) => ({ default: module.Explorer })))
const FeedbackPage = lazy(() => import('./pages/Records').then((module) => ({ default: module.FeedbackPage })))
const ParticipantPage = lazy(() => import('./pages/Records').then((module) => ({ default: module.ParticipantPage })))
const ScanPage = lazy(() => import('./pages/Records').then((module) => ({ default: module.ScanPage })))
const SettingsPage = lazy(() => import('./pages/Settings').then((module) => ({ default: module.SettingsPage })))

function Unavailable({ title, reason }: { title: string; reason: string }) {
  return <><PageHeading title={title} /><section className="panel message-panel"><p>{reason}</p></section></>
}

function Authorized({ session }: { session: Session }) {
  const [revision, setRevision] = useState(0)
  const schemaRequest = useRequest(getSchema, revision)
  const schema = schemaRequest.data
  const views = useMemo(() => studyViews(schema ?? []), [schema])

  if (schemaRequest.loading) return <AuthFrame><Loading label="Checking administrator access…" /></AuthFrame>
  if (!schema) {
    const denied = schemaRequest.error instanceof DashboardError && schemaRequest.error.kind === 'access'
    return <AuthFrame>
      <h1>{denied ? 'Access restricted' : 'Dashboard unavailable'}</h1>
      <ErrorState error={schemaRequest.error} retry={() => setRevision((n) => n + 1)} />
      <SignOut />
    </AuthFrame>
  }

  return <Suspense fallback={<AuthFrame><Loading label="Loading dashboard…" /></AuthFrame>}>
    <Routes>
      <Route element={<Layout views={views} email={session.user.email ?? ''} />}>
        <Route index element={<Overview views={views} tables={schema} />} />
        <Route path="database" element={<Explorer tables={schema} refreshSchema={() => setRevision((n) => n + 1)} />} />
        <Route path="participants" element={views.participants
          ? <ParticipantPage table={views.participants} sessions={views.sessions} />
          : <Unavailable title="Participants" reason="No approved participant table was discovered. Session and installation IDs do not identify individual participants." />} />
        <Route path="scans" element={views.scans ? <ScanPage view={views.scans} />
          : <Unavailable title="Scan Records" reason="No approved scan or prediction table was discovered." />} />
        <Route path="feedback" element={views.feedback ? <FeedbackPage view={views.feedback} />
          : <Unavailable title="Feedback" reason="No approved feedback table was discovered." />} />
        <Route path="analytics" element={views.feedback || views.scans ? <Analytics views={views} />
          : <Unavailable title="Analytics" reason="No supported scan or feedback metrics are available." />} />
        <Route path="settings" element={<SettingsPage email={session.user.email ?? ''} />} />
        <Route path="login" element={<Navigate to="/" replace />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  </Suspense>
}

function SessionGate() {
  const session = useSession()
  if (session === undefined) return <AuthFrame><Loading label="Restoring your session…" /></AuthFrame>
  if (session) return <Authorized key={session.user.id} session={session} />
  return <Routes>
    <Route path="login" element={<Login />} />
    <Route path="*" element={<Navigate to="/login" replace />} />
  </Routes>
}

export default function App() {
  return <HashRouter><SessionGate /></HashRouter>
}
