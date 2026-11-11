import { ShieldCheck } from 'lucide-react'
import { SignOut } from '../components/Auth'
import { PageHeading } from '../components/Ui'
import { projectHost } from '../lib/supabase'

export function SettingsPage({ email }: { email: string }) {
  return <>
    <PageHeading title="Settings" description="Administrator account and application information." />
    <div className="settings-grid">
      <section className="panel settings-card">
        <h2><ShieldCheck size={21} /> Administrator account</h2>
        <dl><dt>Signed in as</dt><dd>{email}</dd><dt>Access</dt><dd>Read-only study dashboard</dd></dl>
        <SignOut />
      </section>
      <section className="panel settings-card">
        <h2>BananaQ Admin</h2>
        <p>Study records and participant feedback for BananaQ, the mobile application for banana leaf disease identification.</p>
        <dl><dt>Supabase project</dt><dd className="break-text">{projectHost}</dd><dt>Application</dt><dd>Web dashboard</dd></dl>
      </section>
    </div>
  </>
}
