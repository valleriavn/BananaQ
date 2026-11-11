import { createClient } from '@supabase/supabase-js'
import { configurationError } from './config'

const url = import.meta.env.VITE_SUPABASE_URL?.trim()
const key = import.meta.env.VITE_SUPABASE_ANON_KEY?.trim()

export const configError = configurationError(url, key)
export const projectHost = url && !configError ? new URL(url).hostname : null
export const supabase = url && key && !configError
  ? createClient(url, key, {
    auth: {
      persistSession: true,
      autoRefreshToken: true,
      detectSessionInUrl: false,
      storageKey: 'bananaq-admin-session',
    },
  })
  : null
