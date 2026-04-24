function isEmptyValue(value) {
  return Array.isArray(value)
    ? value.length === 0
    : value === null || value === undefined || value === ''
}

function normalizeValidators(field) {
  if (Array.isArray(field?.validator)) {
    return field.validator
  }
  if (field?.validator && typeof field.validator === 'object') {
    return [field.validator]
  }
  return []
}

export function applySchemaDefaults(schema, target, { readonlySource = {}, preserveExisting = true } = {}) {
  if (!Array.isArray(schema)) {
    return
  }
  for (const field of schema) {
    if (!field?.field) {
      continue
    }
    const currentValue = target[field.field]
    if (preserveExisting && !isEmptyValue(currentValue)) {
      continue
    }
    if (field.readonly && readonlySource[field.field] !== undefined) {
      target[field.field] = readonlySource[field.field]
      continue
    }
    if (field.defaultValue !== undefined) {
      target[field.field] = field.defaultValue
      continue
    }
    if (field.component === 'switch') {
      target[field.field] = false
      continue
    }
    target[field.field] = ''
  }
}

export function validateSchemaForm(schema, values) {
  const errors = {}
  if (!Array.isArray(schema)) {
    return { valid: true, errors }
  }
  for (const field of schema) {
    if (!field?.field || field.readonly) {
      continue
    }
    const value = values[field.field]
    if (field.required && isEmptyValue(value)) {
      errors[field.field] = field.requiredMessage || `请先填写 ${field.label || field.field}`
      continue
    }
    for (const rule of normalizeValidators(field)) {
      if (!rule || typeof rule !== 'object') {
        continue
      }
      if (rule.type === 'required' && isEmptyValue(value)) {
        errors[field.field] = rule.message || `请先填写 ${field.label || field.field}`
        break
      }
      if (isEmptyValue(value)) {
        continue
      }
      if (rule.type === 'min') {
        const numeric = Number(value)
        if (!Number.isNaN(numeric) && numeric < Number(rule.value)) {
          errors[field.field] = rule.message || `${field.label || field.field} 不能小于 ${rule.value}`
          break
        }
      }
      if (rule.type === 'max') {
        const numeric = Number(value)
        if (!Number.isNaN(numeric) && numeric > Number(rule.value)) {
          errors[field.field] = rule.message || `${field.label || field.field} 不能大于 ${rule.value}`
          break
        }
      }
      if (rule.type === 'pattern') {
        try {
          const regex = new RegExp(rule.value)
          if (!regex.test(String(value))) {
            errors[field.field] = rule.message || `${field.label || field.field} 格式不正确`
            break
          }
        } catch {
          continue
        }
      }
    }
  }
  return {
    valid: Object.keys(errors).length === 0,
    errors
  }
}
