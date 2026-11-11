import { BarChart3, ChevronRight, CircleUserRound, Database, Home, Menu, MessageCircle, ScanLine, Settings, UsersRound, X } from 'lucide-react'
import { useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import type { StudyViews } from '../lib/schema'
import { Brand, Status } from './Ui'

export function Layout({ views, email }: { views: StudyViews; email: string }) {
  const [mobileOpen, setMobileOpen] = useState(false)
  const [online, setOnline] = useState(navigator.onLine)
  const { pathname } = useLocation()
  useEffect(() => {
    const update = () => setOnline(navigator.onLine)
    window.addEventListener('online', update)
    window.addEventListener('offline', update)
    return () => {
      window.removeEventListener('online', update)
      window.removeEventListener('offline', update)
    }
  }, [])
  const routes = [
    { to: '/', label: 'Overview', Icon: Home, enabled: true },
    { to: '/database', label: 'Database Explorer', Icon: Database, enabled: true },
    { to: '/participants', label: 'Participants', Icon: UsersRound, enabled: !!views.participants },
    { to: '/scans', label: 'Scan Records', Icon: ScanLine, enabled: !!views.scans },
    { to: '/feedback', label: 'Feedback', Icon: MessageCircle, enabled: !!views.feedback },
    { to: '/analytics', label: 'Analytics', Icon: BarChart3, enabled: !!(views.scans || views.feedback) },
    { to: '/settings', label: 'Settings', Icon: Settings, enabled: true },
  ]
  const section = routes.find((route) => route.to === pathname)?.label ?? 'Dashboard'

  return <div className="shell">
    <a href="#content" className="skip-link" onClick={(event) => {
      event.preventDefault()
      document.getElementById('content')?.focus()
    }}>Skip to content</a>
    <aside className="sidebar">
      <div className="sidebar-brand">
        <Brand />
        <button type="button" className="menu-button" aria-controls="primary-nav" aria-expanded={mobileOpen}
          aria-label={mobileOpen ? 'Close menu' : 'Open menu'} onClick={() => setMobileOpen(!mobileOpen)}>
          {mobileOpen ? <X size={22} /> : <Menu size={22} />}
        </button>
      </div>
      <nav className={mobileOpen ? 'navigation open' : 'navigation'} id="primary-nav" aria-label="Dashboard">
        {routes.filter((route) => route.enabled).map(({ to, label, Icon }) =>
          <NavLink end key={to} to={to} onClick={() => setMobileOpen(false)}><Icon size={20} />{label}</NavLink>
        )}
      </nav>
      <NavLink className="sidebar-account" to="/settings" title={email}>
        <CircleUserRound size={21} /><span>Admin account</span><ChevronRight size={17} />
      </NavLink>
    </aside>
    <div className="workspace">
      <header className="topbar">
        <div className="breadcrumbs"><NavLink to="/">BananaQ Admin</NavLink><ChevronRight size={15} /><span>{section}</span></div>
        <Status label={online ? 'Connected to database' : 'Device offline'} positive={online} />
      </header>
      <main className="workspace-body" id="content" tabIndex={-1}><Outlet /></main>
    </div>
  </div>
}
