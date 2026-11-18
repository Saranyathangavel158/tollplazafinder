import { LoaderCircle, MapPinned, Route, TriangleAlert } from 'lucide-react'

function Panel({ icon, title, children, tone = 'neutral' }) {
  const tones = {
    neutral: 'border-gray-200 bg-gray-50 text-gray-600',
    error: 'border-red-200 bg-red-50 text-red-700',
  }
  return (
    <div className={`rounded-lg border px-4 py-5 text-sm ${tones[tone]}`}>
      <div className="flex items-start gap-3">
        <span className="mt-0.5 shrink-0">{icon}</span>
        <div>
          <p className="font-medium">{title}</p>
          {children && <p className="mt-1 text-sm opacity-90">{children}</p>}
        </div>
      </div>
    </div>
  )
}

export function IdleState({ onTryExample }) {
  return (
    <Panel icon={<Route className="h-5 w-5 text-brand" aria-hidden="true" />} title="Plan your route">
      Enter a source and a destination PIN code to see the toll plazas along the driving route.{' '}
      <button
        type="button"
        onClick={onTryExample}
        className="font-medium text-brand underline-offset-2 hover:underline focus:outline-none focus-visible:underline"
      >
        Try 560064 to 600001
      </button>
    </Panel>
  )
}

export function LoadingState() {
  return (
    <div role="status" aria-live="polite">
      <Panel
        icon={<LoaderCircle className="h-5 w-5 animate-spin text-brand" aria-hidden="true" />}
        title="Finding toll plazas..."
      >
        Looking up the route. This can take a few seconds the first time.
      </Panel>
    </div>
  )
}

export function EmptyState() {
  return (
    <Panel
      icon={<MapPinned className="h-5 w-5 text-gray-500" aria-hidden="true" />}
      title="No toll plazas found on this route"
    >
      None of the toll plazas in the dataset lie along the driving route between these PIN codes.
    </Panel>
  )
}

export function ErrorState({ message }) {
  return (
    <div role="alert">
      <Panel
        tone="error"
        icon={<TriangleAlert className="h-5 w-5" aria-hidden="true" />}
        title="Something went wrong"
      >
        {message}
      </Panel>
    </div>
  )
}
