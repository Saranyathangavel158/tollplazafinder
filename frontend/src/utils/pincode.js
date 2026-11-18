// Mirrors the backend rules: exactly six digits, first digit 1-9, source and destination differ.
const PINCODE_PATTERN = /^[1-9][0-9]{5}$/

export const PINCODE_FORMAT_MESSAGE = 'PIN code must be exactly 6 digits and must not start with 0'

export function validatePincode(value) {
  if (!value) return 'PIN code is required'
  if (!PINCODE_PATTERN.test(value)) return PINCODE_FORMAT_MESSAGE
  return null
}

/** Returns { source?: string, destination?: string }; an empty object means the pair is valid. */
export function validatePincodePair(source, destination) {
  const errors = {}
  const sourceError = validatePincode(source)
  const destinationError = validatePincode(destination)
  if (sourceError) errors.source = sourceError
  if (destinationError) errors.destination = destinationError
  if (!sourceError && !destinationError && source === destination) {
    errors.destination = 'Source and destination PIN codes must not be identical'
  }
  return errors
}

/** Maps backend field names to the form's field names. */
export function mapServerFieldErrors(fieldErrors = []) {
  const mapped = {}
  for (const { field, message } of fieldErrors) {
    if (field === 'sourcePincode') mapped.source = message
    if (field === 'destinationPincode') mapped.destination = message
  }
  return mapped
}

export function digitsOnly(value) {
  return value.replace(/\D/g, '').slice(0, 6)
}
