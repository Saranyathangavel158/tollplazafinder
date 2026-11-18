package com.tollplaza.tollplazafinder.dataset;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Reads the toll plaza CSV, validates every row, and removes exact duplicates.
 * The source file is only read, never modified.
 */
@Component
public class TollPlazaCsvLoader {

    private static final Logger log = LoggerFactory.getLogger(TollPlazaCsvLoader.class);

    static final List<String> EXPECTED_HEADER = List.of("longitude", "latitude", "toll_name", "geo_state");

    public TollPlazaLoadResult load(Resource resource) {
        if (resource == null || !resource.exists()) {
            throw new TollPlazaDataException("Toll plaza CSV not found: "
                    + (resource == null ? "null" : resource.getDescription()));
        }

        String content;
        try (InputStream in = resource.getInputStream()) {
            content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new TollPlazaDataException("Unable to read toll plaza CSV " + resource.getDescription()
                    + ": " + e.getMessage(), e);
        }

        List<List<String>> rows;
        try {
            rows = parseCsv(content);
        } catch (IllegalArgumentException e) {
            throw new TollPlazaDataException("Malformed toll plaza CSV " + resource.getDescription()
                    + ": " + e.getMessage(), e);
        }

        if (rows.isEmpty()) {
            throw new TollPlazaDataException("Toll plaza CSV is empty: " + resource.getDescription());
        }
        validateHeader(rows.get(0), resource);

        List<TollPlaza> plazas = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int total = 0;
        int invalid = 0;
        int duplicates = 0;

        for (int i = 1; i < rows.size(); i++) {
            int lineNo = i + 1; // 1-based record number including header
            total++;
            List<String> fields = rows.get(i);
            try {
                TollPlaza plaza = toPlaza(fields);
                String key = String.join("\u0000", fields.get(0).trim(), fields.get(1).trim(),
                        plaza.name(), plaza.state());
                if (seen.add(key)) {
                    plazas.add(plaza);
                } else {
                    duplicates++;
                }
            } catch (IllegalArgumentException e) {
                invalid++;
                log.warn("Skipping invalid record {} in {}: {}", lineNo, resource.getDescription(), e.getMessage());
            }
        }

        log.info("Toll plaza CSV loaded from {}: total rows={}, loaded={}, invalid rows skipped={}, duplicate rows removed={}",
                resource.getDescription(), total, plazas.size(), invalid, duplicates);
        return new TollPlazaLoadResult(List.copyOf(plazas), total, invalid, duplicates);
    }

    private void validateHeader(List<String> header, Resource resource) {
        List<String> normalized = header.stream()
                .map(h -> h.replace("﻿", "").trim().toLowerCase())
                .toList();
        if (!normalized.equals(EXPECTED_HEADER)) {
            throw new TollPlazaDataException("Unexpected CSV header in " + resource.getDescription()
                    + ": expected " + EXPECTED_HEADER + " but found " + header);
        }
    }

    private TollPlaza toPlaza(List<String> fields) {
        if (fields.size() != EXPECTED_HEADER.size()) {
            throw new IllegalArgumentException("expected " + EXPECTED_HEADER.size()
                    + " fields but found " + fields.size() + " " + fields);
        }
        double longitude = parseCoordinate(fields.get(0), "longitude", -180, 180);
        double latitude = parseCoordinate(fields.get(1), "latitude", -90, 90);
        String name = fields.get(2).trim();
        String state = fields.get(3).trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("toll_name is blank");
        }
        if (state.isEmpty()) {
            throw new IllegalArgumentException("geo_state is blank");
        }
        return new TollPlaza(longitude, latitude, name, state);
    }

    private double parseCoordinate(String raw, String field, double min, double max) {
        String value = raw.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(field + " is blank");
        }
        double parsed;
        try {
            parsed = Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(field + " is not a number: '" + value + "'");
        }
        if (Double.isNaN(parsed) || Double.isInfinite(parsed) || parsed < min || parsed > max) {
            throw new IllegalArgumentException(field + " out of range [" + min + ", " + max + "]: " + value);
        }
        return parsed;
    }

    /**
     * Minimal RFC 4180 parser: comma separated, double-quote quoting, "" as an escaped quote,
     * quoted fields may contain commas and line breaks. Blank lines are ignored.
     */
    static List<List<String>> parseCsv(String content) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean fieldWasQuoted = false;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < content.length() && content.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"' && field.toString().isBlank()) {
                inQuotes = true;
                fieldWasQuoted = true;
                field.setLength(0);
            } else if (c == ',') {
                row.add(field.toString());
                field.setLength(0);
                fieldWasQuoted = false;
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < content.length() && content.charAt(i + 1) == '\n') {
                    i++;
                }
                if (!row.isEmpty() || !field.isEmpty() || fieldWasQuoted) {
                    row.add(field.toString());
                    rows.add(row);
                }
                row = new ArrayList<>();
                field.setLength(0);
                fieldWasQuoted = false;
            } else {
                field.append(c);
            }
        }
        if (inQuotes) {
            throw new IllegalArgumentException("unterminated quoted field at end of file");
        }
        if (!row.isEmpty() || !field.isEmpty() || fieldWasQuoted) {
            row.add(field.toString());
            rows.add(row);
        }
        return rows;
    }
}
