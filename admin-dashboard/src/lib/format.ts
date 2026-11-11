import { isDate, isEpoch } from './schema'
import type { Field } from './schema'
import type { RecordValue } from './data'

export const countFormat = new Intl.NumberFormat()
const dateFormat = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' })

export function formatCell(value: RecordValue | undefined, field: Field) {
  if (value == null || value === '') return '—'
  if (typeof value === 'boolean') return value ? 'Yes' : 'No'
  if (field.type === 'date' && typeof value === 'string') return value
  if (isDate(field) && (typeof value === 'string' || typeof value === 'number')) {
    const time = new Date(isEpoch(field) ? Number(value) : value)
    return Number.isNaN(time.valueOf()) ? String(value) : dateFormat.format(time)
  }
  if (typeof value === 'number' && ['confidence_score', 'confidence_percent'].includes(field.name)) {
    return countFormat.format(field.name === 'confidence_percent' ? value : value * 100) + '%'
  }
  return typeof value === 'object' ? JSON.stringify(value) : String(value)
}
