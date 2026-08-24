package com.apitest.ddulo.domain.station.service;

import com.apitest.ddulo.domain.station.domain.Station;
import com.apitest.ddulo.domain.station.dto.internal.StationAdjacencyResult;
import com.apitest.ddulo.domain.station.repository.StationRepository;
import com.apitest.ddulo.domain.station.repository.TransferRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StationAdjacencyServiceTest {

    @InjectMocks
    private StationAdjacencyService stationAdjacencyService;

    @Mock
    private StationRepository stationRepository;
    @Mock
    private TransferRepository transferRepository;

    @Test
    @DisplayName("인접역 조회 성공")
    void getAdjacentStations_Success() {
        // Given
        Station station = Station.builder().stationCode("221").stationName("역삼").lineName("2호선").build();
        when(stationRepository.findByStationCode("221")).thenReturn(Optional.of(station));
        
        // When
        StationAdjacencyResult result = stationAdjacencyService.getAdjacentStations("221");

        // Then
        assertNotNull(result);
    }
}
