//package com.apitest.ddulo.domain.timetable.service;
//
//import com.apitest.ddulo.domain.metadata.service.ApiMetadataService;
//import com.apitest.ddulo.domain.station.domain.Station;
//import com.apitest.ddulo.domain.station.repository.StationRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class TimeTableSyncService {
//    private static final String API_NAME = "SEOUL_TIMETABLE";
//
//    private final StationRepository stationRepository;
//    private final TimeTableUpdateService timeTableUpdateService;
//    private final ApiMetadataService apiMetadataService;
//
//    //전체 동기화
//    public void syncAllTimeTables() {
//        if (!apiMetadataService.isApiCallNeeded(API_NAME)) {
//            log.info("timetable 데이터가 이미 존재하므로, 다음으로 넘어갑니다.");
//            return;
//        }
//        log.info("timetable 데이터를 찾을 수 없으므로, 동기화를 실행합니다.");
//
//        // seoul station code가 있는 역들만 가져옴 (데이터 없는 역은 호출 불가하므로)
//        List<Station> targetStations = stationRepository.findAll().stream()
//                .filter(s -> s.getSeoulStationCode() != null && !s.getSeoulStationCode().isEmpty())
//                .toList();
//
//        int successCount = 0;
//        int failCount = 0;
//
//        log.info("총 {}개 역의 시간표 동기화를 시작합니다.", targetStations.size());
//
//        for (Station station : targetStations) {
//            try {
//                timeTableUpdateService.syncTimeTableForStation(station);
//                successCount++;
//            } catch (Exception e) {
//                failCount++;
//                // 한 역이 실패해도 다른 역은 계속 진행하도록 로그만 찍고 넘어감
//                log.error("Failed to sync timetable for station: {} ({})", station.getStationName(), station.getSeoulStationCode(), e);
//            }
//            log.info("성공: {}, 실패: {}", successCount, failCount);
//        }
//        apiMetadataService.updateMetadata(API_NAME);
//        //최종결과 출력(모니터링 good)
//        log.info("전체 시간표 동기화 완료. (총: {}, 성공: {}, 실패: {})", targetStations.size(), successCount, failCount);
//    }
//}
