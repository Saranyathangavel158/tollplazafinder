import { formatKm } from '../utils/format.js'

function Stat({ label, value }) {
  return (
    <div className="rounded-lg border border-gray-200 bg-white px-4 py-3">
      <p className="text-[0.8125rem] font-medium text-gray-500">{label}</p>
      <p className="mt-0.5 text-xl leading-7 font-semibold tracking-tight text-gray-900 tabular-nums">{value}</p>
    </div>
  )
}

export default function RouteSummary({ data }) {
  return (
    <section aria-label="Route summary">
      <p className="mb-2 text-sm text-gray-600">
        <span className="font-semibold tracking-[0.08em] text-gray-900 tabular-nums">{data.sourcePincode}</span>
        <span className="mx-2 text-gray-400">to</span>
        <span className="font-semibold tracking-[0.08em] text-gray-900 tabular-nums">{data.destinationPincode}</span>
      </p>
      <div className="grid grid-cols-2 gap-3">
        <Stat label="Route distance" value={formatKm(data.routeDistanceKm)} />
        <Stat label="Toll plazas" value={data.tollPlazas.length} />
      </div>
    </section>
  )
}
