package com.apitest.ddulo.domain.station.facade;

import com.apitest.ddulo.domain.station.service.StationDataSyncService;
import com.apitest.ddulo.domain.timetable.service.TimeTableFullSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StationSyncFacade {
    private final StationDataSyncService stationDataSyncService;
    private final TimeTableFullSyncService timeTableFullSyncService;

    public void initializeStationData() {
        log.info("전체 역 데이터 동기화 작업을 시작합니다.");
        // 기본 역 정보 적재 (station.csv)
        stationDataSyncService.syncStationsIfNeeded();
        // 서울시 역 코드 매핑 업데이트 (seoul_station_code.csv)
        stationDataSyncService.syncSeoulStationCodesIfNeeded();

        new Thread(() -> {
            log.info("시간표 동기화 백그라운드 작업 시작...");
            timeTableFullSyncService.syncAdditionalTimeTable();
        }).start();

        log.info("기본 데이터 로딩 완료. 시간표는 백그라운드에서 계속 다운로드됩니다.");
    }
}
