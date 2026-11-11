import type { Field, Table, View } from './schema'
import { isEpoch } from './schema'
import { supabase } from './supabase'

export type RecordValue = string | number | boolean | null | Record<string, unknown> | unknown[]
export type Row = { record: Record<string, RecordValue>, related: Record<string, RecordValue> | null }
export type Page = { total: number, rows: Row[] }
export type Point = { label: string, count: number }

type Params = Record<string, unknown>

export class DashboardError extends Error {
  constructor(readonly kind: 'access' | 'setup' | 'connection' | 'query') {
    super({
      access: 'Administrator access is required for this account.',
      setup: 'Schema discovery is unavailable. Ask the project owner to install the dashboard SQL and assign administrator access.',
      connection: 'Could not reach Supabase. Check the connection and try again.',
      query: 'This query could not be completed. Refresh the page or ask the project owner to review the dashboard setup.',
    }[kind])
  }
}

async function rpc<T>(name: string, params: Params, signal: AbortSignal): Promise<T> {
  if (!supabase) throw new DashboardError('setup')
  const { data, error, status } = await supabase.rpc(name, params).abortSignal(signal)
  if (error) {
    const kind = error.code === 'PGRST202' || error.code === '42883' ? 'setup'
      : error.code === '42501' || status === 401 || status === 403 ? 'access'
        : !status || status >= 500 ? 'connection' : 'query'
    throw new DashboardError(kind)
  }
  return data as T
}

export function getSchema(signal: AbortSignal) {
  return rpc<Table[]>('get_dashboard_schema', {}, signal)
}

export function getData<T>(view: Pick<View, 'table' | 'relation'>, params: Params, signal: AbortSignal) {
  return rpc<T>('get_dashboard_data', {
    p_table: view.table.name,
    ...(view.relation ? { p_fk: view.relation.name } : {}),
    ...params,
  }, signal)
}

export function getTotal(table: Table, signal: AbortSignal) {
  return getData<{ total: number }>({ table }, { p_mode: 'count' }, signal).then(({ total }) => total)
}

export function getSeries(
  view: View, field: Field, mode: 'distribution' | 'daily' | 'confidence', signal: AbortSignal,
  options: { limit?: number, days?: number } = {},
) {
  return getData<Point[]>(view, {
    p_mode: mode,
    p_field: field.key,
    p_limit: options.limit ?? 12,
    p_days: options.days ?? 30,
    p_epoch_ms: isEpoch(field),
    p_percent: field.name === 'confidence_percent',
  }, signal)
}

export function valueOf(row: Row, field: Field): RecordValue | undefined {
  return (field.related ? row.related : row.record)?.[field.name]
}

export function errorMessage(error: unknown): string {
  return error instanceof DashboardError ? error.message : 'Something went wrong. Please try again.'
}
