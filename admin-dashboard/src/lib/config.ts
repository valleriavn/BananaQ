export function configurationError(url?: string, key?: string): string | null {
  if (!url || !key) return 'Set VITE_SUPABASE_URL and VITE_SUPABASE_ANON_KEY in the dashboard environment, then rebuild.'

  const error = 'Use the HTTPS Supabase project URL and a publishable or legacy anon key.'
  try {
    const parsed = new URL(url)
    if (parsed.protocol !== 'https:' || parsed.pathname !== '/' ||
        parsed.username || parsed.password || parsed.search || parsed.hash) return error
    if (/^sb_publishable_[\w-]+$/.test(key)) return null
    const segments = key.split('.')
    if (segments.length !== 3) return error
    const payload = segments[1].replace(/-/g, '+').replace(/_/g, '/')
    return JSON.parse(atob(payload)).role === 'anon' ? null : error
  } catch {
    return error
  }
}
