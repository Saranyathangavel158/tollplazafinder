package com.tollplaza.tollplazafinder.dataset;

/**
 * Thrown when the toll plaza dataset cannot be loaded at all
 * (missing file, unreadable file, or wrong header).
 */
public class TollPlazaDataException extends RuntimeException {

    public TollPlazaDataException(String message) {
        super(message);
    }

    public TollPlazaDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
