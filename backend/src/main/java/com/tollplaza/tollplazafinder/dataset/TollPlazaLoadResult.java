package com.tollplaza.tollplazafinder.dataset;

import java.util.List;

/**
 * Outcome of a CSV load.
 *
 * @param plazas            validated, de-duplicated records
 * @param totalRows         data rows read (header excluded)
 * @param invalidRows       rows skipped because they were malformed or failed validation
 * @param duplicatesRemoved rows dropped because an identical record was already loaded
 */
public record TollPlazaLoadResult(List<TollPlaza> plazas, int totalRows, int invalidRows, int duplicatesRemoved) {
}
