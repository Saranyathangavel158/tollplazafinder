package com.tollplaza.tollplazafinder.geocoding;

/**
 * Thrown when a PIN code cannot be geocoded (no result, API error, timeout,
 * malformed response, or missing configuration).
 * Messages never contain the request URL, so the API key cannot leak through them.
 */
public class GeocodingException extends RuntimeException {

    public GeocodingException(String message) {
        super(message);
    }
}
