import { useEffect, useState } from 'react'

type Request<T> = (signal: AbortSignal) => Promise<T>
type Result<T> = { data?: T; error?: unknown }

export function useRequest<T>(request: Request<T>, revision = 0) {
  const [state, setState] = useState<{ request: Request<T>; revision: number; result: Result<T> }>()
  useEffect(() => {
    const controller = new AbortController()
    const finish = (result: Result<T>) => {
      if (!controller.signal.aborted) setState({ request, revision, result })
    }
    void Promise.resolve().then(() => request(controller.signal))
      .then((data) => finish({ data }), (error: unknown) => finish({ error }))
    return () => controller.abort()
  }, [request, revision])

  const result = state?.request === request && state.revision === revision ? state.result : undefined
  return { ...result, loading: !result }
}
