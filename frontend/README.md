# Toll Plaza Finder - Frontend

React single-page app for the Toll Plaza Finder backend. Enter a source and destination Indian PIN code and see the toll plazas on the driving route, as a list and as markers on a map.

**Stack:** React 19, Vite, JavaScript, Tailwind CSS 4, Leaflet + React Leaflet, Axios, Lucide React.

## Run locally

Prerequisites: Node.js 20+ (built with Node 22) and the backend running on port 8080 (see `../backend/README.md`).

```powershell
npm install
npm run dev
```

Open the URL Vite prints (normally <http://localhost:5173>).

Other scripts: `npm run build` (production build into `dist/`), `npm run preview`, `npm run lint`.

## Configuration

Copy `.env.example` to `.env` to change the defaults.

| Variable | Default | Purpose |
|---|---|---|
| `VITE_API_BASE_URL` | empty | Base URL of the backend API. Empty means same-origin requests (`/api/...`). |
| `VITE_DEV_PROXY_TARGET` | `http://localhost:8080` | Where `npm run dev` forwards `/api` requests. |

**CORS:** the backend has no CORS configuration. During development the Vite dev server proxies `/api` to the backend, so the browser never makes a cross-origin request. If you set `VITE_API_BASE_URL` to a different origin, the backend must first allow that origin. For a production deployment, serve the built files and the API from the same origin (for example behind a reverse proxy).

The frontend never contains or needs the Geoapify API key; it stays in the backend's `GEOAPIFY_API_KEY` environment variable.

## What the UI shows

- Source and destination PIN code inputs (digits only, six characters) with a swap button
- Client-side validation that mirrors the backend rules, plus backend field errors if they come back
- Route distance and the number of toll plazas
- A list of toll plazas in travel order: name, coordinates and distance from the source
- A Leaflet map with a numbered marker and popup per toll plaza; selecting a list item or a marker highlights both
- Loading, empty, validation and error states

The backend response contains only toll plaza data (`routeDistanceKm` and `tollPlazas`). It has no route geometry and no source or destination coordinates, so the map shows toll markers only and does not draw a route line.

## Structure

```
src/
├── api/tollPlazaApi.js       Axios client, request function, error normalisation
├── hooks/useTollSearch.js    search state (idle / loading / success / error) and cancellation
├── utils/                    PIN code validation, number/coordinate formatting
├── components/               SearchForm, PincodeInput, RouteSummary, TollList, TollMap, StatusMessages
├── App.jsx                   page layout and wiring
└── main.jsx, index.css       entry point and Tailwind theme
```

## Attribution

Map tiles: &copy; [OpenStreetMap](https://www.openstreetmap.org/copyright) contributors. Routing and geocoding data come from the backend, which uses Geoapify.
