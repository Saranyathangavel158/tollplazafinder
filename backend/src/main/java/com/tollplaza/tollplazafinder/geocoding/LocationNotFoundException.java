package com.tollplaza.tollplazafinder.geocoding;

/**
 * Geoapify answered normally but found no location for the PIN code.
 * Kept separate from other geocoding failures so the API can answer 404 instead of 502.
 */
public class LocationNotFoundException extends GeocodingException {

    public LocationNotFoundException(String message) {
        super(message);
    }
}
