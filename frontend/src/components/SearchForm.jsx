import { ArrowUpDown, LoaderCircle, MapPin, Search } from 'lucide-react'
import PincodeInput from './PincodeInput.jsx'

export default function SearchForm({
  source,
  destination,
  errors,
  loading,
  onSourceChange,
  onDestinationChange,
  onSwap,
  onSubmit,
}) {
  const handleSubmit = (event) => {
    event.preventDefault()
    onSubmit()
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="space-y-3">
      <div className="flex items-start gap-2">
        <div className="flex-1 space-y-3">
          <PincodeInput
            id="source-pincode"
            label="Source PIN code"
            placeholder="e.g. 560064"
            value={source}
            error={errors.source}
            onChange={onSourceChange}
            icon={<MapPin className="h-4 w-4 text-brand" aria-hidden="true" />}
          />
          <PincodeInput
            id="destination-pincode"
            label="Destination PIN code"
            placeholder="e.g. 600001"
            value={destination}
            error={errors.destination}
            onChange={onDestinationChange}
            icon={<MapPin className="h-4 w-4 text-red-600" aria-hidden="true" />}
          />
        </div>
        <button
          type="button"
          onClick={onSwap}
          aria-label="Swap source and destination"
          title="Swap source and destination"
          className="mt-[1.85rem] flex h-10 w-10 shrink-0 items-center justify-center self-start rounded-full border border-gray-300 bg-white text-gray-600 transition hover:bg-gray-50 hover:text-brand focus:outline-none focus-visible:ring-2 focus-visible:ring-brand"
        >
          <ArrowUpDown className="h-4 w-4" aria-hidden="true" />
        </button>
      </div>

      <button
        type="submit"
        disabled={loading}
        className="flex w-full items-center justify-center gap-2 rounded-lg bg-brand px-4 py-2.5 text-[0.9375rem] font-medium text-white transition hover:bg-brand-dark focus:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-70"
      >
        {loading ? (
          <>
            <LoaderCircle className="h-4 w-4 animate-spin" aria-hidden="true" />
            Finding toll plazas...
          </>
        ) : (
          <>
            <Search className="h-4 w-4" aria-hidden="true" />
            Find Toll Plazas
          </>
        )}
      </button>
    </form>
  )
}
