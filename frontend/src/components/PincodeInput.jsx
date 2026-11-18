export default function PincodeInput({ id, label, placeholder, value, error, onChange, icon }) {
  const errorId = `${id}-error`

  return (
    <div>
      <label htmlFor={id} className="mb-1.5 block text-label font-medium text-gray-700">
        {label}
      </label>
      <div className="relative">
        <span className="pointer-events-none absolute inset-y-0 left-3 flex items-center">{icon}</span>
        <input
          id={id}
          type="text"
          inputMode="numeric"
          autoComplete="postal-code"
          maxLength={6}
          placeholder={placeholder}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? errorId : undefined}
          className={`w-full rounded-lg border bg-white py-2.5 pr-3 pl-10 text-base font-medium tracking-[0.14em] text-gray-900 tabular-nums placeholder:font-normal placeholder:tracking-normal placeholder:text-gray-400 focus:outline-none focus:ring-2 ${
            error
              ? 'border-red-400 focus:border-red-500 focus:ring-red-200'
              : 'border-gray-300 focus:border-brand focus:ring-brand-soft'
          }`}
        />
      </div>
      {error && (
        <p id={errorId} role="alert" className="mt-1.5 text-[0.8125rem] leading-5 text-red-600">
          {error}
        </p>
      )}
    </div>
  )
}
