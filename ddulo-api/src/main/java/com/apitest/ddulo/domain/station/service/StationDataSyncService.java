package com.apitest.ddulo.domain.station.service;

import com.apitest.ddulo.domain.metadata.domain.ApiMetadata;
import com.apitest.ddulo.domain.metadata.repository.ApiMetadataRepository;
import com.apitest.ddulo.domain.metadata.service.ApiMetadataService;
import com.apitest.ddulo.domain.station.domain.Station;
import com.apitest.ddulo.domain.station.repository.StationRepository;
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
public class StationDataSyncService {

    private static final String API_NAME = "PUZZLE_STATION_META";
    private static final String SEOUL_CODE_META_NAME = "SEOUL_STATION_CODE_UPDATE";

    private final StationRepository stationRepository;
    private final ApiMetadataService apiMetadataService;

    //station 데이터 적재 로직
    @Transactional
    public void syncStationsIfNeeded() {

        // 가져올 필요 있는지 판단
        if (!apiMetadataService.isApiCallNeeded(API_NAME)) {
            log.info("Station 데이터가 이미 존재하므로, 다음으로 넘어갑니다.");
            return;
        }
        log.info("Station 데이터를 찾을 수 없으므로, 동기화를 실행합니다.");

        // CSV 데이터 로드
        loadStationCsvData();

        //성공 후 메타데이터 갱신 (없으면 생성)
        apiMetadataService.updateMetadata(API_NAME);
    }

    //station 데이터에 서울시 api의 역코드 추가 업데이트 로직
    @Transactional
    public void syncSeoulStationCodesIfNeeded() {
        // 이미 실행했는지 메타데이터 확인
        if (!apiMetadataService.isApiCallNeeded(SEOUL_CODE_META_NAME)) {
            log.info("Seoul Station Code 업데이트가 이미 완료되어 생략합니다.");
            return;
        }
        log.info("Seoul Station Code 업데이트를 시작합니다.");

        // CSV 데이터 로드
        updateSeoulStationCodesFromCsv();

        //성공 후 메타데이터 갱신 (없으면 생성)
        apiMetadataService.updateMetadata(SEOUL_CODE_META_NAME);
    }

    // CSV를 읽어 기존 Station에 seoulStationCode를 매핑하여 업데이트
    private void updateSeoulStationCodesFromCsv() {
        ClassPathResource resource = new ClassPathResource("data/seoul_station_code.csv");

        try (CSVReader csvReader = new CSVReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            // 성능을 위해 기존의 모든 Station을 조회하여 Map으로 변환 (Key: stationCode, Value: Station)
            // 이렇게 하면 CSV 한 줄마다 DB 조회를 할 필요가 없어짐 (N+1 문제 방지)
            List<Station> allStations = stationRepository.findAll();
            Map<String, Station> stationMap = allStations.stream()
                    .collect(Collectors.toMap(Station::getStationCode, Function.identity(), (oldValue, newValue) -> oldValue));

            // CSV 읽기
            List<String[]> allRows = csvReader.readAll();
            int updatedCount = 0;

            // CSV 파싱 및 업데이트 (i = 1 부터 시작하여 헤더 스킵)
            for (int i = 1; i < allRows.size(); i++) {
                String[] row = allRows.get(i);

                // CSV 구조:
                // 0:"전철역코드", 1:"전철역명", 2:"전철명명(영문)", 3:"호선", 4:"외부코드", ...
                if (row.length < 5) continue;

                String seoulCode = row[0];   // 매핑할 값 (예: "1722")
                String externalCode = row[4]; // 매핑 기준 키 (예: "P163")

                // Map에서 찾아오기 (DB 조회 X)
                Station station = stationMap.get(externalCode);

                if (station != null) {
                    // 엔티티 값 변경 -> @Transactional에 의해 메서드 종료 시 자동 update 쿼리 나감 (Dirty Checking)
                    station.updateSeoulStationCode(seoulCode);
                    updatedCount++;
                }
            }

            log.info("총 {}건 중 {}건의 SeoulStationCode 업데이트 완료", allRows.size() - 1, updatedCount);

        } catch (Exception e) {
            log.error("Seoul Station Code CSV 업데이트 중 오류 발생", e);
            throw new RuntimeException("서울 역코드 업데이트 실패");
        }
    }

    // station CSV 로드 로직
    private void loadStationCsvData() {
        ClassPathResource resource = new ClassPathResource("data/station.csv");

        try (CSVReader csvReader = new CSVReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            // 헤더 포함 모든 라인 읽기
            List<String[]> allRows = csvReader.readAll();
            List<Station> stationList = new ArrayList<>();

            // 파싱 (i = 1 부터 시작해서 헤더 스킵)
            for (int i = 1; i < allRows.size(); i++) {
                String[] row = allRows.get(i);

                // 빈 줄 방지
                if (row.length < 7) continue;

                // CSV 컬럼 인덱스 매핑
                // 0:id, 1:created_at, 2:lat, 3:line, 4:lon, 5:code, 6:name, 7:updated_at

                String lineName = row[3];      // "1호선"
                String stationCode = row[5];   // "100-1" (String이어야 함!)
                String stationName = row[6];   // "소요산역"

                // 위도, 경도 파싱 (빈 값이면 0.0 처리 등 방어 로직 추가 가능)
                double lat = parseDoubleOrDefault(row[2]);
                double lon = parseDoubleOrDefault(row[4]);

                // 엔티티 생성
                Station station = Station.builder()
                        .lineName(lineName)
                        .stationCode(stationCode)
                        .stationName(stationName)
                        .latitude(lat)
                        .longitude(lon)
                        .build();

                stationList.add(station);
            }

            // DB 저장
            stationRepository.saveAll(stationList);
            log.info("총 {}건의 Station 데이터 저장 완료", stationList.size());

        } catch (Exception e) {
            log.error("CSV 초기화 중 오류 발생", e);
            // throw new RuntimeException("데이터 초기화 실패");
        }
    }

    //위경도 파싱 로직
    private double parseDoubleOrDefault(String value) {
        // null이거나, 빈 문자열이거나, 공백만 있는 경우
        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }

        try {
            // 정상 파싱 시도
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            // 숫자가 아닌 이상한 문자(예: "unknown")가 들어왔을 때도 0.0 처리
            return 0.0;
        }
    }
}
