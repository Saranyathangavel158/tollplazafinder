import { useMemo, useState } from 'react'
import { MapPin } from 'lucide-react'
import SearchForm from './components/SearchForm.jsx'
import RouteSummary from './components/RouteSummary.jsx'
import TollList from './components/TollList.jsx'
import TollMap from './components/TollMap.jsx'
import { EmptyState, ErrorState, IdleState, LoadingState } from './components/StatusMessages.jsx'
import { useTollSearch } from './hooks/useTollSearch.js'
import { digitsOnly, mapServerFieldErrors, validatePincodePair } from './utils/pincode.js'

const NO_TOLLS = []
const EXAMPLE = { source: '560064', destination: '600001' }

export default function App() {
  const [source, setSource] = useState('')
  const [destination, setDestination] = useState('')
  const [clientErrors, setClientErrors] = useState({})
  const [selectedIndex, setSelectedIndex] = useState(null)
  const { status, data, error, search } = useTollSearch()

  const serverErrors = useMemo(() => mapServerFieldErrors(error?.fieldErrors), [error])
  const fieldErrors = { ...serverErrors, ...clientErrors }
  const hasFieldErrors = Boolean(fieldErrors.source || fieldErrors.destination)

  const tollPlazas = data?.tollPlazas ?? NO_TOLLS
  const loading = status === 'loading'

  const runSearch = (from, to) => {
    const errors = validatePincodePair(from, to)
    setClientErrors(errors)
    if (Object.keys(errors).length > 0) return
    setSelectedIndex(null)
    search(from, to)
  }

  const handleSourceChange = (value) => {
    setSource(digitsOnly(value))
    setClientErrors((prev) => ({ ...prev, source: undefined, destination: undefined }))
  }

  const handleDestinationChange = (value) => {
    setDestination(digitsOnly(value))
    setClientErrors((prev) => ({ ...prev, destination: undefined }))
  }

  const handleSwap = () => {
    setSource(destination)
    setDestination(source)
    setClientErrors({})
  }

  const handleTryExample = () => {
    setSource(EXAMPLE.source)
    setDestination(EXAMPLE.destination)
    runSearch(EXAMPLE.source, EXAMPLE.destination)
  }

  return (
    <div className="flex min-h-dvh flex-col lg:h-dvh lg:flex-row">
      <aside className="flex w-full flex-col border-b border-gray-200 bg-white lg:h-full lg:w-[26rem] lg:shrink-0 lg:border-r lg:border-b-0">
        <header className="flex items-center gap-3 border-b border-gray-200 px-5 py-4">
          <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-brand text-white shadow-sm">
            <MapPin className="h-[18px] w-[18px]" strokeWidth={2.25} aria-hidden="true" />
          </span>
          <div className="min-w-0">
            <h1 className="text-[1.0625rem] leading-6 font-semibold tracking-tight text-gray-900">
              Toll Plaza <span className="text-brand">Finder</span>
            </h1>
            <p className="text-[0.8125rem] leading-5 text-gray-500">Toll plazas between two Indian PIN codes</p>
          </div>
        </header>

        <div className="space-y-5 overflow-y-auto px-5 py-5">
          <SearchForm
            source={source}
            destination={destination}
            errors={fieldErrors}
            loading={loading}
            onSourceChange={handleSourceChange}
            onDestinationChange={handleDestinationChange}
            onSwap={handleSwap}
            onSubmit={() => runSearch(source, destination)}
          />

          {status === 'idle' && <IdleState onTryExample={handleTryExample} />}
          {loading && <LoadingState />}
          {status === 'error' && !hasFieldErrors && <ErrorState message={error.message} />}
          {status === 'success' && (
            <>
              <RouteSummary data={data} />
              {tollPlazas.length === 0 ? (
                <EmptyState />
              ) : (
                <TollList tollPlazas={tollPlazas} selectedIndex={selectedIndex} onSelect={setSelectedIndex} />
              )}
            </>
          )}
        </div>
      </aside>

      <main className="relative h-[60vh] min-h-80 w-full lg:h-full lg:min-h-0 lg:flex-1">
        <TollMap tollPlazas={tollPlazas} selectedIndex={selectedIndex} onSelect={setSelectedIndex} />
      </main>
    </div>
  )
}
