package com.apitest.ddulo.domain.timetable.service;

import com.apitest.ddulo.domain.metadata.service.ApiMetadataService;
import com.apitest.ddulo.domain.station.domain.Station;
import com.apitest.ddulo.domain.station.repository.StationRepository;
import com.apitest.ddulo.domain.timetable.domain.TimeTable;
import com.apitest.ddulo.domain.timetable.repository.TimeTableBulkRepository;
import com.apitest.ddulo.domain.timetable.repository.TimeTableRepository;
import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeTableFullSyncService {
    private static final String API_NAME = "SEOUL_TIMETABLE_FULL";

    private final StationRepository stationRepository;
    private final TimeTableRepository timeTableRepository;
    private final ApiMetadataService apiMetadataService;
    private final TimeTableBulkRepository timeTableBulkRepository;

    //1, 2호선 시간표 추가 적재 로직
    public void syncAdditionalTimeTable() {
        if (!apiMetadataService.isApiCallNeeded(API_NAME)) {
            log.info("timetable full 데이터가 이미 존재하므로, 다음으로 넘어갑니다.");
            return;
        }
        log.info("timetable full 데이터를 찾을 수 없으므로, 동기화를 실행합니다.");

        // 성능 최적화: DB의 모든 역을 가져와서 Map으로 캐싱
        // Key: seoulStationCode (CSV의 '역사코드'와 일치)
        Map<String, Station> stationMap = stationRepository.findAll().stream()
                .filter(s -> s.getSeoulStationCode() != null) // 코드가 있는 역만
                .collect(Collectors.toMap(Station::getSeoulStationCode, Function.identity(), (o1, o2) -> o1));

        ClassPathResource resource = new ClassPathResource("data/additional_timetable.csv");

        try (CSVReader csvReader = new CSVReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            List<String[]> allRows = csvReader.readAll();
            int totalRows = allRows.size() - 1; // 헤더 제외한 전체 데이터 수
            int successCount = 0;
            int failCount = 0;
            int processedCount = 0;

            List<TimeTable> batchList = new ArrayList<>();

            // i=1 (헤더 스킵)
            for (int i = 1; i < allRows.size(); i++) {
                String[] row = allRows.get(i);
                processedCount++;

                // CSV 컬럼 인덱스
                // 0:고유번호, 1:호선, 2:역사코드, 3:역사명, 4:주중주말, 5:방향
                // 6:급행여부, 7:열차코드, 8:도착시간, 9:출발시간, 10:출발역, 11:도착역
                String codeInCsv = row[2]; // "0150"
                // Map에서 바로 조회
                Station station = stationMap.get(codeInCsv);

                if (station == null) {
                    failCount++;
                    // log.warn("매핑된 역이 없음: {} ({})", row[3], codeInCsv);
                    continue;
                }

                // 엔티티 생성 및 리스트 추가
                batchList.add(mapRowToEntity(row, station));
                successCount++;

                // 1000개씩 끊어서 저장 (Batch Insert)
                if (batchList.size() >= 5000) {
//                    timeTableRepository.saveAll(batchList);
                    timeTableBulkRepository.saveAllBatch(batchList);
                    batchList.clear();

                    log.info("진행률: {}/{} ({}%) - 성공: {}, 실패/스킵: {}",
                            processedCount, totalRows, (int)((double)processedCount/totalRows * 100), successCount, failCount);
                }
            }

            // 남은 데이터 저장
            if (!batchList.isEmpty()) {
                timeTableBulkRepository.saveAllBatch(batchList);
            }

            log.info("==========================================");
            log.info("1~9호선 열차 시간표 적재 완료 결과");
            log.info("총 데이터: {}", totalRows);
            log.info("성공(저장됨): {}", successCount);
            log.info("실패(스킵됨): {}", failCount);
            log.info("==========================================");

            apiMetadataService.updateMetadata(API_NAME);

        } catch (Exception e) {
            log.error("추가 데이터 로딩 실패", e);
            throw new RuntimeException(e);
        }
    }

    //데이터 매핑 및 번역
    private TimeTable mapRowToEntity(String[] row, Station station) {
        // 호선 처리: "1" -> "1호선"
        String lineName = row[1].matches("\\d+") ? row[1] + "호선" : row[1];

        return TimeTable.builder()
                .station(station)
                .lineNum(lineName)
                .seoulStationCode(station.getSeoulStationCode()) // DB값 사용
                .stationName(row[3])          // "서울역"

                // 변환 로직 적용
                .weekTag(convertWeekTag(row[4]))     // DAY -> 1
                .inOutTag(convertInOutTag(row[5]))   // DOWN -> 2
                .expressYn(convertExpress(row[6]))   // 0 -> G

                .frCode(station.getStationCode())    // "150"
                .trainNo(row[7])              // "K101"
                .arriveTime(row[8])           // "12:53:00"
                .leftTime(row[9])             // "12:53:30"
                .originStationName(row[10])   // "양주"
                .destStationName(row[11])     // "인천"
                .build();
    }

    //변환 헬퍼 메서드

    private String convertWeekTag(String val) {
        if ("DAY".equals(val)) return "1"; // 평일
        if ("SAT".equals(val)) return "2"; // 토요일
        if ("END".equals(val)) return "3"; // 휴일
        return "-1";
    }

    private String convertInOutTag(String val) {
        // UP: 상행/내선(1), DOWN: 하행/외선(2)
        if ("UP".equals(val) || "IN".equals(val)) return "1";
        if ("DOWN".equals(val) || "OUT".equals(val)) return "2";
        return "-1";
    }

    private String convertExpress(String val) {
        // 0: 일반(G), 1: 급행(D)
        if ("1".equals(val)) return "D";
        if ("0".equals(val)) return "G";
        return null;
    }
}
