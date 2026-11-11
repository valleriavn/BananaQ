export interface Column {
  name: string
  type: string
  nullable: boolean
  primaryKey: boolean
}

export interface Relationship {
  name: string
  columns: string[]
  targetSchema: string
  targetTable: string
  targetColumns: string[]
  readable: boolean
}

export interface Table {
  name: string
  columns: Column[]
  relationships: Relationship[]
}

export interface Field extends Column {
  key: string
  label: string
  related?: boolean
}

export interface View {
  table: Table
  relation?: Relationship
  fields: Field[]
  label?: Field
  rating?: Field
  result?: Field
  date?: Field
  confidence?: Field
  comment?: Field
}

export const humanize = (key: string) => key.replace(/_/g, ' ').replace(/^./, (c) => c.toUpperCase())

export function toFields(table: Table, related = false): Field[] {
  return table.columns.map((column) => ({
    ...column,
    key: related ? 'related.' + column.name : column.name,
    label: humanize(column.name),
    related,
  }))
}

const labelNames = ['predicted_disease', 'predicted_label', 'disease_label', 'prediction_label']
const confidenceNames = ['confidence_percent', 'confidence_score']
const resultNames = ['result_valid', 'is_valid', 'valid_prediction', 'prediction_valid']
const dateNames = ['submitted_at', 'submitted_at_ms', 'received_at', 'created_at', 'timestamp']
const textType = (field: Field) => /^(text|character|citext)/i.test(field.type)
const numericType = (field: Field) => /^(real|double precision|numeric|decimal|integer|bigint|smallint)/i.test(field.type)
const temporalType = (field: Field) => /^(date|timestamp)/i.test(field.type) ||
  (field.name.endsWith('_ms') && numericType(field))
export const isEpoch = (field: Field) => field.name.endsWith('_ms') && numericType(field)
export const isDate = temporalType

function pick(fields: Field[], names: string[], check: (field: Field) => boolean = () => true) {
  return names.map((name) => fields.find((field) => field.name.toLowerCase() === name && check(field))).find(Boolean)
}

function findTable(schema: Table[], names: string[], supports: (table: Table) => boolean) {
  return names.map((name) => schema.find((table) => table.name.toLowerCase() === name && supports(table))).find(Boolean)
}

export function scanView(table: Table): View {
  const fields = toFields(table)
  return {
    table, fields,
    label: pick(fields, labelNames, textType),
    confidence: pick(fields, confidenceNames, numericType),
    date: pick(fields, ['scanned_at', 'predicted_at', ...dateNames], temporalType),
    result: pick(fields, resultNames, (field) => field.type === 'boolean'),
  }
}

export function studyViews(schema: Table[]) {
  const scanTable = findTable(schema, ['scans', 'scan_records', 'predictions', 'prediction_records'],
    (table) => table.columns.some((column) => column.primaryKey) ||
      Boolean(pick(toFields(table), labelNames, textType) || pick(toFields(table), ['scanned_at', 'predicted_at'], temporalType)))
  const feedbackTable = findTable(schema, ['feedback', 'scan_feedback', 'participant_feedback'],
    (table) => table.columns.length > 0)
  const participantTable = findTable(schema, ['participants', 'study_participants', 'participant_profiles', 'user_profiles', 'users'],
    (table) => table.columns.some((column) => column.primaryKey))
  const sessionTable = findTable(schema, ['user_sessions', 'sessions', 'study_sessions'], (table) => table.columns.length > 0)

  let feedback: View | undefined
  if (feedbackTable) {
    const fields = toFields(feedbackTable)
    const candidates = feedbackTable.relationships.filter((key) =>
      key.readable && key.targetSchema === 'public' && key.targetTable === scanTable?.name)
    const relation = candidates.find((key) => key.columns.some((name) => ['scan_id', 'prediction_id'].includes(name)))
      ?? (candidates.length === 1 ? candidates[0] : undefined)
    const related = relation && scanTable ? toFields(scanTable, true) : []
    const hasOwn = (field: Field) => fields.some((own) => own.name === field.name)
    feedback = {
      table: feedbackTable, relation, fields: [...fields, ...related.filter((field) => !hasOwn(field))],
      label: pick(fields, labelNames, textType) ?? pick(related, labelNames, textType),
      confidence: pick(fields, confidenceNames, numericType) ?? pick(related, confidenceNames, numericType),
      rating: pick(fields, ['accuracy_rating', 'rating', 'feedback_rating']),
      result: pick(fields, resultNames, (field) => field.type === 'boolean') ??
        pick(related, resultNames, (field) => field.type === 'boolean'),
      date: pick(fields, dateNames, temporalType),
      comment: pick(fields, ['comments', 'comment', 'message', 'feedback_text'], textType),
    }
  }

  return {
    scans: scanTable ? scanView(scanTable) : undefined,
    feedback,
    participants: participantTable,
    sessions: sessionTable,
  }
}

export type StudyViews = ReturnType<typeof studyViews>

export function labelField(field: Field | undefined, label: string): Field[] {
  return field ? [{ ...field, label }] : []
}
