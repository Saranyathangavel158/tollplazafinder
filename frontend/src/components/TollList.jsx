import { formatCoordinates, formatKm } from '../utils/format.js'

export default function TollList({ tollPlazas, selectedIndex, onSelect }) {
  return (
    <section aria-label="Toll plazas on the route">
      <h2 className="mb-2 text-[0.6875rem] font-semibold tracking-wider text-gray-500 uppercase">
        Along the route, in travel order
      </h2>
      <ol className="space-y-2">
        {tollPlazas.map((toll, index) => {
          const selected = index === selectedIndex
          return (
            <li key={`${toll.tollPlazaName}-${toll.latitude}-${toll.longitude}`}>
              <button
                type="button"
                onClick={() => onSelect(index)}
                aria-pressed={selected}
                className={`flex w-full items-start gap-3 rounded-lg border px-3 py-3 text-left transition focus:outline-none focus-visible:ring-2 focus-visible:ring-brand ${
                  selected ? 'border-brand bg-brand-soft' : 'border-gray-200 bg-white hover:bg-gray-50'
                }`}
              >
                <span className="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-brand text-xs font-semibold text-white">
                  {index + 1}
                </span>
                <span className="min-w-0 flex-1">
                  <span className="block text-sm leading-5 font-medium text-gray-900">{toll.tollPlazaName}</span>
                  <span className="mt-0.5 block text-[0.8125rem] leading-5 text-gray-500 tabular-nums">
                    {formatCoordinates(toll.latitude, toll.longitude)}
                  </span>
                </span>
                <span className="shrink-0 text-right">
                  <span className="block text-sm leading-5 font-semibold text-brand tabular-nums">
                    {formatKm(toll.distanceFromSourceKm)}
                  </span>
                  <span className="block text-[0.6875rem] leading-4 text-gray-500">from source</span>
                </span>
              </button>
            </li>
          )
        })}
      </ol>
    </section>
  )
}
