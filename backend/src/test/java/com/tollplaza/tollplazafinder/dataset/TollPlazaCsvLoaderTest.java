package com.tollplaza.tollplazafinder.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

class TollPlazaCsvLoaderTest {

    private static final String HEADER = "longitude,latitude,toll_name,geo_state\n";

    private final TollPlazaCsvLoader loader = new TollPlazaCsvLoader();

    private static Resource csv(String body) {
        return new ByteArrayResource((HEADER + body).getBytes(StandardCharsets.UTF_8), "test.csv");
    }

    @Test
    void loadsValidRowsIncludingQuotedFields() {
        TollPlazaLoadResult result = loader.load(csv(
                "74.99,23.41,Dhamnod Toll Plaza,Madhya Pradesh\n"
                + "79.61,22.07,\"Seoni, Plaza \"\"A\"\"\",Madhya Pradesh\n"));

        assertEquals(2, result.totalRows());
        assertEquals(0, result.invalidRows());
        assertEquals(0, result.duplicatesRemoved());
        assertEquals(new TollPlaza(74.99, 23.41, "Dhamnod Toll Plaza", "Madhya Pradesh"), result.plazas().get(0));
        assertEquals("Seoni, Plaza \"A\"", result.plazas().get(1).name());
    }

    @Test
    void removesOnlyExactDuplicates() {
        TollPlazaLoadResult result = loader.load(csv(
                "77.485,23.1415,11 Miles Toll Plaza,Madhya Pradesh\n"
                + "77.485,23.1415,11 Miles Toll Plaza,Madhya Pradesh\n"
                + "77.485,23.1415,11 Miles Toll Plaza,Gujarat\n"      // different state -> kept
                + "77.486,23.1415,11 Miles Toll Plaza,Madhya Pradesh\n")); // different longitude -> kept

        assertEquals(4, result.totalRows());
        assertEquals(1, result.duplicatesRemoved());
        assertEquals(3, result.plazas().size());
    }

    @Test
    void skipsInvalidCoordinates() {
        TollPlazaLoadResult result = loader.load(csv(
                "abc,23.0,Bad Lon Text,State\n"
                + "181.0,23.0,Lon Too Big,State\n"
                + "77.0,91.0,Lat Too Big,State\n"
                + "77.0,-90.5,Lat Too Small,State\n"
                + "77.0,23.0,Good Plaza,State\n"));

        assertEquals(5, result.totalRows());
        assertEquals(4, result.invalidRows());
        assertEquals(1, result.plazas().size());
        assertEquals("Good Plaza", result.plazas().get(0).name());
    }

    @Test
    void skipsRowsWithMissingFields() {
        TollPlazaLoadResult result = loader.load(csv(
                "77.0,23.0,,State\n"            // blank name
                + "77.0,23.0,Name,   \n"          // blank state
                + ",23.0,Name,State\n"            // blank longitude
                + "77.0,23.0,Name\n"              // too few fields
                + "77.0,23.0,Name,State,Extra\n"  // too many fields
                + "77.0,23.0,Ok,State\n"));

        assertEquals(6, result.totalRows());
        assertEquals(5, result.invalidRows());
        assertEquals(1, result.plazas().size());
    }

    @Test
    void failsClearlyWhenResourceIsMissing() {
        TollPlazaDataException ex = assertThrows(TollPlazaDataException.class,
                () -> loader.load(new ClassPathResource("data/does_not_exist.csv")));
        assertTrue(ex.getMessage().contains("not found"));
        assertTrue(ex.getMessage().contains("does_not_exist.csv"));
    }

    @Test
    void failsClearlyOnWrongHeaderOrEmptyFile() {
        Resource wrongHeader = new ByteArrayResource("lat,lon,name,state\n1,2,a,b\n".getBytes(StandardCharsets.UTF_8), "bad.csv");
        assertTrue(assertThrows(TollPlazaDataException.class, () -> loader.load(wrongHeader))
                .getMessage().contains("Unexpected CSV header"));

        Resource empty = new ByteArrayResource(new byte[0], "empty.csv");
        assertTrue(assertThrows(TollPlazaDataException.class, () -> loader.load(empty))
                .getMessage().contains("empty"));
    }

    @Test
    void failsOnUnterminatedQuote() {
        assertThrows(TollPlazaDataException.class, () -> loader.load(csv("77.0,23.0,\"Open,State\n")));
    }

    @Test
    void loadsRealDataset() {
        TollPlazaLoadResult result = loader.load(new ClassPathResource("data/toll_plaza_india.csv"));

        assertEquals(2385, result.totalRows());
        assertEquals(0, result.invalidRows());
        assertEquals(849, result.duplicatesRemoved());
        assertEquals(1536, result.plazas().size());
    }
}
