import { useEffect, useMemo, useRef } from 'react'
import L from 'leaflet'
import { MapContainer, Marker, Popup, TileLayer, useMap } from 'react-leaflet'
import { formatCoordinates, formatKm } from '../utils/format.js'

const INDIA_CENTER = [22.5, 79]
const INDIA_ZOOM = 5

function markerIcon(number, active) {
  return L.divIcon({
    className: '',
    html: `<div class="toll-marker${active ? ' toll-marker--active' : ''}">${number}</div>`,
    iconSize: [30, 30],
    iconAnchor: [15, 15],
    popupAnchor: [0, -16],
  })
}

/** Keeps the viewport on the toll markers whenever the result set changes. */
function FitToMarkers({ tollPlazas }) {
  const map = useMap()

  useEffect(() => {
    if (tollPlazas.length === 0) {
      map.setView(INDIA_CENTER, INDIA_ZOOM)
    } else if (tollPlazas.length === 1) {
      map.setView([tollPlazas[0].latitude, tollPlazas[0].longitude], 11)
    } else {
      const bounds = L.latLngBounds(tollPlazas.map((t) => [t.latitude, t.longitude]))
      map.fitBounds(bounds, { padding: [48, 48] })
    }
  }, [map, tollPlazas])

  return null
}

/** Opens the popup of the selected toll and pans to it. */
function FocusSelected({ tollPlazas, selectedIndex, markerRefs }) {
  const map = useMap()

  useEffect(() => {
    if (selectedIndex == null) return
    const toll = tollPlazas[selectedIndex]
    if (!toll) return
    map.flyTo([toll.latitude, toll.longitude], Math.max(map.getZoom(), 10), { duration: 0.6 })
    markerRefs.current[selectedIndex]?.openPopup()
  }, [map, tollPlazas, selectedIndex, markerRefs])

  return null
}

export default function TollMap({ tollPlazas, selectedIndex, onSelect }) {
  const markerRefs = useRef([])

  const icons = useMemo(
    () => tollPlazas.map((_, index) => markerIcon(index + 1, index === selectedIndex)),
    [tollPlazas, selectedIndex],
  )

  return (
    <div className="relative h-full w-full">
      <MapContainer center={INDIA_CENTER} zoom={INDIA_ZOOM} scrollWheelZoom className="h-full w-full">
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <FitToMarkers tollPlazas={tollPlazas} />
        <FocusSelected tollPlazas={tollPlazas} selectedIndex={selectedIndex} markerRefs={markerRefs} />

        {tollPlazas.map((toll, index) => (
          <Marker
            key={`${toll.tollPlazaName}-${toll.latitude}-${toll.longitude}`}
            position={[toll.latitude, toll.longitude]}
            icon={icons[index]}
            ref={(marker) => {
              markerRefs.current[index] = marker
            }}
            eventHandlers={{ click: () => onSelect(index) }}
          >
            <Popup>
              <div className="min-w-40">
                <p className="text-sm font-semibold text-gray-900">{toll.tollPlazaName}</p>
                <p className="mt-1 text-xs text-gray-600">
                  {formatKm(toll.distanceFromSourceKm)} from source
                </p>
                <p className="text-xs text-gray-500">{formatCoordinates(toll.latitude, toll.longitude)}</p>
              </div>
            </Popup>
          </Marker>
        ))}
      </MapContainer>

      <p className="pointer-events-none absolute bottom-6 left-3 z-[500] max-w-[16rem] rounded-md bg-white/90 px-2.5 py-1.5 text-[11px] leading-snug text-gray-600 shadow-sm ring-1 ring-gray-200">
        Markers show toll plaza locations. The route line is not drawn because the API does not return it.
      </p>
    </div>
  )
}
