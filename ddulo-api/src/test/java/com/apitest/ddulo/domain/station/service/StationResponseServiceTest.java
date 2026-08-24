package com.apitest.ddulo.domain.station.service;

import com.apitest.ddulo.domain.station.domain.Station;
import com.apitest.ddulo.domain.station.dto.external.redis.StationDetailData;
import com.apitest.ddulo.domain.station.dto.internal.StationAdjacencyResult;
import com.apitest.ddulo.domain.station.dto.response.StationArrivalResponse;
import com.apitest.ddulo.domain.station.repository.StationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StationResponseServiceTest {

    @InjectMocks
    private StationResponseService stationResponseService;

    @Mock
    private StationRepository stationRepository;

    @Test
    @DisplayName("응답 DTO 생성 테스트")
    void buildStationArrivalResponse_Success() {
        // Given
        StationDetailData.StationInfo redisStationInfo = new StationDetailData.StationInfo();
        redisStationInfo.setStationId("221");
        
        StationDetailData redisData = StationDetailData.builder()
                .station(redisStationInfo)
                .upBound(Collections.emptyList())
                .downBound(Collections.emptyList())
                .build();

        StationAdjacencyResult adjacencyResult = StationAdjacencyResult.builder()
                .prevStations(Collections.emptyList())
                .nextStations(Collections.emptyList())
                .build();

        Station dbStation = Station.builder().stationCode("221").stationName("역삼").lineName("2호선").build();
        when(stationRepository.findByStationCode("221")).thenReturn(Optional.of(dbStation));

        // When
        StationArrivalResponse response = stationResponseService.buildStationArrivalResponse(redisData, adjacencyResult);

        // Then
        assertNotNull(response);
        assertEquals("역삼", response.getStation().getStationName());
        assertEquals("2호선", response.getStation().getLineName());
    }
}
