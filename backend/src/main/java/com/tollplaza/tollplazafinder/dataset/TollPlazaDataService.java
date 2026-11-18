package com.tollplaza.tollplazafinder.dataset;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * Holds the validated toll plaza records in memory for later route matching.
 * The dataset is loaded once at application startup; startup fails with a clear
 * message if the CSV is missing or has the wrong header.
 */
@Service
public class TollPlazaDataService {

    private final TollPlazaCsvLoader loader;
    private final Resource csvResource;
    private volatile TollPlazaLoadResult loaded = new TollPlazaLoadResult(List.of(), 0, 0, 0);

    public TollPlazaDataService(TollPlazaCsvLoader loader,
            @Value("${tollplaza.dataset.path:classpath:data/toll_plaza_india.csv}") Resource csvResource) {
        this.loader = loader;
        this.csvResource = csvResource;
    }

    @PostConstruct
    void init() {
        this.loaded = loader.load(csvResource);
    }

    public List<TollPlaza> getAll() {
        return loaded.plazas();
    }

    public TollPlazaLoadResult getLoadResult() {
        return loaded;
    }
}
