package com.tollplaza.tollplazafinder.dataset;

/**
 * A single validated toll plaza record.
 */
public record TollPlaza(double longitude, double latitude, String name, String state) {
}
