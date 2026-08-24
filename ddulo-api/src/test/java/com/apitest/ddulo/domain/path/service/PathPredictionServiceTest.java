package com.apitest.ddulo.domain.path.service;

import com.apitest.ddulo.domain.path.dto.external.redis.PathPredictionData;
import com.apitest.ddulo.domain.path.dto.response.PathPredictionResponse;
import com.apitest.ddulo.domain.station.dto.response.FastestPathResponse;
import com.apitest.ddulo.domain.station.repository.StationRepository;
import com.apitest.ddulo.domain.timetable.domain.TimeTable;
import com.apitest.ddulo.domain.timetable.repository.TimeTableRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PathPredictionServiceTest {

    @InjectMocks
    private PathPredictionService pathPredictionService;

    @Mock
    private PathRedisService pathRedisService;
    @Mock
    private StationRepository stationRepository;
    @Mock
    private TimeTableRepository timeTableRepository;

    @Test
    @DisplayName("경로 예측 상세 정보 조회 - 정상 케이스")
    void predictPathDetails_Success() {
        // Given
        // 1. 최단 경로 데이터
        FastestPathResponse fastestPathResponse = FastestPathResponse.builder()
                .totalTime(600)
                .legs(List.of(
                        FastestPathResponse.RouteLeg.builder()
                                .startStation("역삼").endStation("강남").lineName("2호선").direction("내선").build()
                ))
                .build();

        // 2. Mock Station Code
        when(stationRepository.findStationCodeByNameAndLine(anyString(), anyString()))
                .thenReturn(Optional.of("221")) // 역삼
                .thenReturn(Optional.of("222")); // 강남

        // 3. Mock Redis Data
        PathPredictionData.ScheduleOption schedule1 = new PathPredictionData.ScheduleOption();
        schedule1.setBoardingProbability(80);
        PathPredictionData.ScheduleOption schedule2 = new PathPredictionData.ScheduleOption();
        schedule2.setBoardingProbability(60);
        
        PathPredictionData.PathResult result = new PathPredictionData.PathResult();
        result.setSchedule(List.of(schedule1, schedule2)); // 예측 정보 2개

        PathPredictionData redisData = PathPredictionData.builder()
                .results(List.of(result))
                .build();

        when(pathRedisService.getPathPrediction(anyString(), anyString())).thenReturn(redisData);

        // 4. Mock TimeTable (3개)
        TimeTable t1 = TimeTable.builder().leftTime("12:00:00").build();
        TimeTable t2 = TimeTable.builder().leftTime("12:10:00").build();
        TimeTable t3 = TimeTable.builder().leftTime("12:20:00").build();

        when(timeTableRepository.findNextTrains(anyString(), anyString(), anyString(), anyString(), any(Pageable.class)))
                .thenReturn(List.of(t1, t2, t3));

        // When
        PathPredictionResponse response = pathPredictionService.predictPathDetails(fastestPathResponse);

        // Then
        assertNotNull(response);
        assertEquals(3, response.getPredictions().size()); // 열차 3개만큼 결과 생성

        // 첫 번째 열차 검증
        assertEquals(80, response.getPredictions().get(0).getBoardingProbability());
        assertEquals(LocalTime.of(12, 0), response.getPredictions().get(0).getEstimatedBoardingTime().toLocalTime());

        // 세 번째 열차 검증 (Redis 데이터 부족 -> 확률 0)
        assertEquals(0, response.getPredictions().get(2).getBoardingProbability());
    }
}
