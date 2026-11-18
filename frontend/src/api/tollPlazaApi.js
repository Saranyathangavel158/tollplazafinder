import axios from 'axios'

// Empty by default: requests go to the same origin and the Vite dev proxy forwards "/api".
const baseURL = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/+$/, '')

const client = axios.create({
  baseURL,
  timeout: 45_000, // the backend makes up to three Geoapify calls on a cache miss
  headers: { 'Content-Type': 'application/json' },
})

/**
 * POST /api/v1/toll-plazas
 * Resolves with the backend's TollPlazaResponse:
 * { sourcePincode, destinationPincode, routeDistanceKm, tollPlazas: [{ tollPlazaName, latitude, longitude, distanceFromSourceKm }] }
 */
export async function findTollPlazas(sourcePincode, destinationPincode, signal) {
  const { data } = await client.post(
    '/api/v1/toll-plazas',
    { sourcePincode, destinationPincode },
    { signal },
  )
  return data
}

export function isCancelled(error) {
  return axios.isCancel(error)
}

const STATUS_MESSAGES = {
  400: 'The request was not valid. Please check the PIN codes.',
  404: 'Nothing was found for these PIN codes.',
  502: 'The routing service is not responding. Please try again shortly.',
}

/**
 * Converts any request failure into { status, message, fieldErrors }.
 * Backend errors look like { status, error, message, errors: [{ field, message }] }.
 */
export function toApiError(error) {
  if (error.response) {
    const { status, data } = error.response
    return {
      status,
      message: data?.message || STATUS_MESSAGES[status] || `The server responded with an error (HTTP ${status}).`,
      fieldErrors: Array.isArray(data?.errors) ? data.errors : [],
    }
  }
  if (error.code === 'ECONNABORTED') {
    return { status: null, message: 'The request timed out. Please try again.', fieldErrors: [] }
  }
  return {
    status: null,
    message: 'Could not reach the server. Make sure the backend is running and try again.',
    fieldErrors: [],
  }
}
