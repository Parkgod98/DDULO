package com.apitest.ddulo.domain.subway.service;

import com.apitest.ddulo.domain.metadata.domain.ApiMetadata;
import com.apitest.ddulo.domain.metadata.repository.ApiMetadataRepository;
import com.apitest.ddulo.domain.station.domain.Station;
import com.apitest.ddulo.domain.station.repository.StationRepository;
import com.apitest.ddulo.domain.subway.domain.TrainCongestion;
import com.apitest.ddulo.domain.subway.dto.CongestionDataDto;
import com.apitest.ddulo.domain.subway.dto.PuzzleTrainCongestionResponseDto;
import com.apitest.ddulo.domain.subway.dto.StatDto;
import com.apitest.ddulo.global.common.enums.DayOfWeek;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 지하철 혼잡도 API 동기화 서비스
 */
@Service
@RequiredArgsConstructor
public class TrainCongestionSyncService {

    private static final String API_NAME = "PUZZLE_TRAIN_CONGESTION";

    private static final String DEBUG_STATION_CODE = "133"; // 서울역

    private final TrainCongestionApiClient apiClient;
    private final StationRepository stationRepository;
    private final ApiMetadataRepository apiMetadataRepository;
    private final TrainCongestionInsertService insertService;

    private final ObjectMapper objectMapper;

    /**
     * 월 단위 혼잡도 동기화
     */
    public void syncMonthlyCongestion() {

        ApiMetadata metadata = apiMetadataRepository
                .findByApiName(API_NAME)
                .orElseGet(() ->
                        apiMetadataRepository.save(
                                ApiMetadata.builder()
                                        .apiName(API_NAME)
                                        .updateIntervalDay(30)
                                        .build()
                        )
                );

        // 업데이트 주기가 아직 안 됐으면 종료
        if (!metadata.isUpdateDue()) {
            return;
        }

        LocalDate now = LocalDate.now();

        // 페이지 단위로 Station 조회
        int page = 0;
        int size = 50; // 페이지당 50개 역
        Pageable pageable = PageRequest.of(page, size);

        while (true) {
            var stations = stationRepository.findAll(pageable);
            if (stations.isEmpty()) break;

            for (Station station : stations) {

                // 🔥 디버깅용: 특정 역만 처리
//                if (!station.getStationCode().equals(DEBUG_STATION_CODE)) {
//                    continue;
//                }

                for (DayOfWeek dow : DayOfWeek.values()) {
                    for (int hour = 5; hour <= 23; hour++) {
                        try {
                            // 한 역 단위로 트랜잭션 적용, 개별 예외 처리
                            saveStationCongestion(station, dow, hour);
                        } catch (Exception e) {
                            // 개별 역 에러는 WARN 처리
                            System.out.println(
                                    "⚠️ station=" + station.getStationName()
                                            + " 처리 중 에러 발생: " + e.getMessage()
                            );
                        }
                    }
                }
            }

            page++;
            pageable = PageRequest.of(page, size);
        }

        // 메타데이터 업데이트 시간 갱신
        metadata.markUpdated();
    }

    /**
     * 특정 역의 혼잡도 데이터 저장
     */
    @Transactional
    public void saveStationCongestion(Station station, DayOfWeek dow, int hour) {

        PuzzleTrainCongestionResponseDto response =
                apiClient.fetchCongestion(station.getStationCode(), dow, hour);

        if (response == null || response.getContents() == null) return;

        // 통계 시작/종료 날짜
        LocalDate statStartDate = LocalDate.parse(
                response.getContents().getStatStartDate(),
                DateTimeFormatter.BASIC_ISO_DATE
        );

        LocalDate statEndDate = LocalDate.parse(
                response.getContents().getStatEndDate(),
                DateTimeFormatter.BASIC_ISO_DATE
        );

        /**
         * 🔑 핵심 포인트
         * - 같은 API 응답 안에서도
         *   (station, direction, statStartDate, measuredAt, dayOfWeek)
         *   조합이 중복해서 내려오는 경우가 있음
         *
         * 👉 DB insert 전에 Service 레벨에서 1차 dedup 수행
         */
        Map<String, TrainCongestion> congestionMap = new HashMap<>();

        for (StatDto stat : response.getContents().getStat()) {
            int direction = stat.getUpdnLine();

            for (CongestionDataDto data : stat.getData()) {

                List<Integer> cars = data.getCongestionCar();
                if (cars == null || cars.isEmpty()) continue;

                // 측정 시간 (HH:mm)
                LocalTime measuredAt = LocalTime.of(
                        Integer.parseInt(data.getHh()),
                        Integer.parseInt(data.getMm())
                );

                // 차량별 혼잡도 JSON 변환
                String carsJson;
                try {
                    carsJson = objectMapper.writeValueAsString(cars);
                } catch (Exception e) {
                    // JSON 직렬화 실패 시 skip
                    continue;
                }

                DayOfWeek dayOfWeek = DayOfWeek.valueOf(data.getDow());

                /**
                 * 유니크 키 생성
                 * (DB unique constraint와 동일한 기준)
                 */
                String uniqueKey =
                        station.getStationId() + "_" +
                                direction + "_" +
                                statStartDate + "_" +
                                measuredAt + "_" +
                                dayOfWeek;

                // 같은 키가 여러 번 들어오면 마지막 값으로 덮어씀
                congestionMap.put(
                        uniqueKey,
                        TrainCongestion.builder()
                                .station(station)
                                .direction(direction)
                                .dayOfWeek(dayOfWeek)
                                .measuredAt(measuredAt)
                                .statStartDate(statStartDate)
                                .statEndDate(statEndDate)
                                .congestionCarsJson(carsJson)
                                .build()
                );
            }
        }

        // Map → List 변환 후 bulk insert
        List<TrainCongestion> newCongestions =
                new ArrayList<>(congestionMap.values());

        insertService.insertCongestions(newCongestions);
    }
}
