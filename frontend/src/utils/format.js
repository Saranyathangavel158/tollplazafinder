const kmFormat = new Intl.NumberFormat('en-IN', { maximumFractionDigits: 1, minimumFractionDigits: 1 })

export function formatKm(value) {
  return `${kmFormat.format(value)} km`
}

export function formatCoordinates(latitude, longitude) {
  return `${latitude.toFixed(5)}, ${longitude.toFixed(5)}`
}
