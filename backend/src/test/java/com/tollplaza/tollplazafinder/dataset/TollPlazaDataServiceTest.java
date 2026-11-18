package com.tollplaza.tollplazafinder.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TollPlazaDataServiceTest {

    @Autowired
    private TollPlazaDataService service;

    @Test
    void datasetIsLoadedIntoMemoryAtStartup() {
        assertEquals(1536, service.getAll().size());
        assertEquals(849, service.getLoadResult().duplicatesRemoved());
    }
}
