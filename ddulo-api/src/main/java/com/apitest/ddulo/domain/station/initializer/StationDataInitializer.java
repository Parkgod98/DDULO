package com.apitest.ddulo.domain.station.initializer;

import com.apitest.ddulo.domain.station.facade.StationSyncFacade;
import com.apitest.ddulo.domain.station.service.StationDataSyncService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StationDataInitializer {

    private final StationSyncFacade stationSyncFacade;

    @PostConstruct
    public void init() {
        stationSyncFacade.initializeStationData();
    }
}
