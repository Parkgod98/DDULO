package com.apitest.ddulo.domain.path.service;

import com.apitest.ddulo.domain.path.dto.external.redis.PathFullData;
import com.apitest.ddulo.domain.path.dto.response.PathPredictionResponse;
import com.apitest.ddulo.domain.path.dto.response.ResultResponse;
import com.apitest.ddulo.domain.station.dto.response.FastestPathResponse;
import com.apitest.ddulo.domain.station.repository.StationRepository;
import com.apitest.ddulo.global.exception.CustomException;
import com.apitest.ddulo.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResultServiceTest {

    @InjectMocks
    private ResultService resultService;

    @Mock
    private PathRedisService pathRedisService;
    @Mock
    private StationRepository stationRepository;

    @Test
    @DisplayName("최종 결과 조회 - 정상 케이스")
    void getResult_Success() {
        // Given
        // 1. 최단 경로 데이터
        FastestPathResponse fastestPathResponse = FastestPathResponse.builder()
                .legs(List.of(
                        FastestPathResponse.RouteLeg.builder()
                                .startStation("역삼").endStation("강남").lineName("2호선").build()
                ))
                .build();

        // 2. 예측 정보 데이터 (더미)
        PathPredictionResponse pathPredictionResponse = PathPredictionResponse.builder().build();

        // 3. Mock Station Code
        when(stationRepository.findStationCodeByNameAndLine(anyString(), anyString()))
                .thenReturn(Optional.of("221")) // 역삼
                .thenReturn(Optional.of("222")); // 강남

        // 4. Mock Redis Data (PathFullData)
        PathFullData pathFullData = PathFullData.builder()
                .totalTimeSecond(600)
                .estimatedBoardingTime("2026-01-28T14:30:00")
                .boardingProbability(80.0)
                .startStation(List.of(
                        new PathFullData.StationInfo("221", "역삼", "2호선", 1, 120, true, null)
                ))
                .endStation(new PathFullData.StationInfo("222", "강남", "2호선", 1, 0, true, null))
                .build();

        when(pathRedisService.getFullPath(anyString(), anyString())).thenReturn(pathFullData);

        // When
        ResultResponse response = resultService.getResult(fastestPathResponse, pathPredictionResponse);

        // Then
        assertNotNull(response);
        assertEquals(600, response.getTotalTimeSecond());
        assertEquals(80.0, response.getBoardingProbability());
        assertEquals("역삼", response.getStartStation().get(0).getStationName());
        assertEquals("강남", response.getEndStation().getStationName());
    }

    @Test
    @DisplayName("최종 결과 조회 - Redis 데이터 없음 (예외 발생)")
    void getResult_DataNotFound() {
        // Given
        FastestPathResponse fastestPathResponse = FastestPathResponse.builder()
                .legs(List.of(
                        FastestPathResponse.RouteLeg.builder()
                                .startStation("역삼").endStation("강남").lineName("2호선").build()
                ))
                .build();
        PathPredictionResponse pathPredictionResponse = PathPredictionResponse.builder().build();

        when(stationRepository.findStationCodeByNameAndLine(anyString(), anyString()))
                .thenReturn(Optional.of("221"))
                .thenReturn(Optional.of("222"));

        when(pathRedisService.getFullPath(anyString(), anyString())).thenReturn(null); // 데이터 없음

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> 
            resultService.getResult(fastestPathResponse, pathPredictionResponse)
        );
        assertEquals(ErrorCode.DATA_NOT_FOUND, exception.getErrorCode());
    }
}
