import { CircleAlert, Database, LoaderCircle, RefreshCw } from 'lucide-react'
import type { ReactNode } from 'react'
import logo from '../assets/logo.png'
import { errorMessage } from '../lib/data'

export function Brand() {
  return <div className="brand"><img src={logo} alt="" width="44" height="44" />
    <span>BananaQ <small>Admin</small></span>
  </div>
}

export function PageHeading({ title, description, actions }: {
  title: string; description?: string; actions?: ReactNode
}) {
  return <div className="page-heading">
    <div><h1>{title}</h1>{description && <p>{description}</p>}</div>
    {actions && <div className="page-actions">{actions}</div>}
  </div>
}

export function RefreshButton({ onClick, loading }: { onClick: () => void; loading?: boolean }) {
  return <button className="button primary" type="button" onClick={onClick} disabled={loading}>
    <RefreshCw size={17} className={loading ? 'spin' : undefined} /> Refresh
  </button>
}

export function Status({ label = 'Connected to database', positive = true }: { label?: string; positive?: boolean }) {
  return <span className={positive ? 'status positive' : 'status'}><i aria-hidden="true" />{label}</span>
}

export function Loading({ label = 'Loading records…' }: { label?: string }) {
  return <div className="state" role="status"><LoaderCircle size={24} className="spin" /> <p>{label}</p></div>
}

export function Empty({ label = 'No records available.', detail }: { label?: string; detail?: string }) {
  return <div className="state"><Database size={25} aria-hidden="true" />
    <p>{label}</p>{detail && <small>{detail}</small>}
  </div>
}

export function ErrorState({ error, retry }: { error: unknown; retry?: () => void }) {
  return <div className="state problem" role="alert"><CircleAlert size={24} />
    <p>{errorMessage(error)}</p>{retry && <button className="button secondary" type="button" onClick={retry}>Try again</button>}
  </div>
}
