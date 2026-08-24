package com.apitest.ddulo.domain.station.initializer;

import com.apitest.ddulo.domain.station.service.ExitDataSyncService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@DependsOn("stationDataInitializer") // Station 데이터가 먼저 초기화되어야 함
public class ExitDataInitializer {

    private final ExitDataSyncService exitDataSyncService;

    @PostConstruct
    public void init() {
        exitDataSyncService.syncExitInfoIfNeeded();
    }
}
