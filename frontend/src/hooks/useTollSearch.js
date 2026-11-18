import { useCallback, useEffect, useRef, useState } from 'react'
import { findTollPlazas, isCancelled, toApiError } from '../api/tollPlazaApi.js'

const IDLE = { status: 'idle', data: null, error: null }

/**
 * Runs a toll plaza search and tracks its state: idle | loading | success | error.
 * A new search cancels the one still in flight.
 */
export function useTollSearch() {
  const [state, setState] = useState(IDLE)
  const controllerRef = useRef(null)

  useEffect(() => () => controllerRef.current?.abort(), [])

  const search = useCallback(async (source, destination) => {
    controllerRef.current?.abort()
    const controller = new AbortController()
    controllerRef.current = controller

    setState({ status: 'loading', data: null, error: null })
    try {
      const data = await findTollPlazas(source, destination, controller.signal)
      setState({ status: 'success', data, error: null })
    } catch (error) {
      if (isCancelled(error)) return
      setState({ status: 'error', data: null, error: toApiError(error) })
    }
  }, [])

  return { ...state, search }
}
