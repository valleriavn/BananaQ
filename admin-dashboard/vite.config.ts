import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import tailwind from '@tailwindcss/vite'
import { configurationError } from './src/lib/config'

export default defineConfig(({ command, mode }) => {
  if (command === 'build') {
    const env = loadEnv(mode, process.cwd(), 'VITE_SUPABASE_')
    const error = configurationError(env.VITE_SUPABASE_URL?.trim(), env.VITE_SUPABASE_ANON_KEY?.trim())
    if (error) throw new Error(error)
  }
  return {
    base: '/BananaQ/',
    plugins: [react(), tailwind()],
    build: {
      rollupOptions: { output: { manualChunks: { 'supabase-client': ['@supabase/supabase-js'] } } },
    },
  }
})
