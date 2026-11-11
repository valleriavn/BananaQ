import { Eye, EyeOff, LockKeyhole, Mail } from 'lucide-react'
import { useEffect, useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import type { Session } from '@supabase/supabase-js'
import { configError, supabase } from '../lib/supabase'
import { Brand } from './Ui'

export function useSession() {
  const [session, setSession] = useState<Session | null | undefined>(supabase ? undefined : null)
  useEffect(() => {
    if (!supabase) return
    const { data } = supabase.auth.onAuthStateChange((_event, next) => setSession(next))
    return () => data.subscription.unsubscribe()
  }, [])
  return session
}

export function AuthFrame({ children }: { children: ReactNode }) {
  return <main className="auth-screen">
    <div className="auth-card">
      <header className="auth-brand"><Brand /></header>
      <div className="auth-body">{children}</div>
    </div>
  </main>
}

export function Login() {
  const [pending, setPending] = useState(false)
  const [visible, setVisible] = useState(false)
  const [error, setError] = useState('')
  async function signIn(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!supabase || pending) return
    const form = new FormData(event.currentTarget)
    setError('')
    setPending(true)
    try {
      const { error: failure } = await supabase.auth.signInWithPassword({
        email: String(form.get('email')).trim(), password: String(form.get('password')),
      })
      if (failure) {
        setError(failure.status === 429
          ? 'Too many sign-in attempts. Wait a moment and try again.'
          : failure.status && failure.status < 500
            ? 'Sign-in failed. Check your email and password.'
            : 'Could not reach Supabase. Try again.')
      }
    } catch {
      setError('Could not reach Supabase. Try again.')
    } finally {
      setPending(false)
    }
  }

  return <AuthFrame>
    <h1>Sign in to BananaQ Admin</h1>
    <p className="auth-intro">Access the administrative dashboard for the BananaQ banana disease study.</p>
    <form onSubmit={signIn} className="auth-form">
      <label>Email
        <span className="icon-input"><Mail size={17} /><input name="email" type="email"
          autoComplete="username" placeholder="Enter your email" maxLength={254} required /></span>
      </label>
      <div className="form-field"><label htmlFor="admin-password">Password</label>
        <span className="icon-input"><LockKeyhole size={17} /><input name="password"
          id="admin-password" type={visible ? 'text' : 'password'} autoComplete="current-password" placeholder="Enter your password" required />
          <button className="icon-button" type="button" onClick={() => setVisible(!visible)}
            aria-label={visible ? 'Hide password' : 'Show password'}>{visible ? <EyeOff size={18} /> : <Eye size={18} />}</button>
        </span>
      </div>
      {(configError || error) && <p className="inline-error" role="alert">{configError ?? error}</p>}
      <button className="button primary" type="submit" disabled={pending || !!configError}>
        {pending ? 'Signing in…' : 'Sign in'}
      </button>
    </form>
  </AuthFrame>
}

export function SignOut() {
  const [pending, setPending] = useState(false)
  const [failed, setFailed] = useState(false)
  async function signOut() {
    setPending(true)
    setFailed(false)
    try {
      const response = await supabase?.auth.signOut({ scope: 'local' })
      if (response?.error) setFailed(true)
    } catch {
      setFailed(true)
    } finally {
      setPending(false)
    }
  }
  return <div>
    <button className="button secondary" type="button" onClick={signOut} disabled={pending}>
      {pending ? 'Signing out…' : 'Sign out'}
    </button>
    {failed && <p role="alert" className="inline-error">Could not sign out. Try again.</p>}
  </div>
}
