package com.apitest.ddulo.domain.path.service;

import com.apitest.ddulo.domain.path.dto.request.PythonPathRequest;
import com.apitest.ddulo.domain.station.domain.Exit;
import com.apitest.ddulo.domain.station.domain.Station;
import com.apitest.ddulo.domain.station.domain.Transfer;
import com.apitest.ddulo.domain.station.dto.response.FastestPathResponse;
import com.apitest.ddulo.domain.station.repository.ExitRepository;
import com.apitest.ddulo.domain.station.repository.StationRepository;
import com.apitest.ddulo.domain.station.repository.TransferRepository;
import com.apitest.ddulo.domain.station.client.PythonApiClient;
import com.apitest.ddulo.domain.timetable.domain.TimeTable;
import com.apitest.ddulo.domain.timetable.repository.TimeTableRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PythonPathServiceTest {

    @InjectMocks
    private PythonPathService pythonPathService;

    @Mock
    private StationRepository stationRepository;
    @Mock
    private ExitRepository exitRepository;
    @Mock
    private TransferRepository transferRepository;
    @Mock
    private TimeTableRepository timeTableRepository;
    @Mock
    private PythonApiClient pythonApiClient;

    @Test
    @DisplayName("경로 계산 요청 트리거 테스트 - 환승 1회 포함")
    void triggerPythonPathCalculationTest() {
        // Given
        // 1. 경로 데이터 (역삼 -> 강남 -> 신사)
        FastestPathResponse response = FastestPathResponse.builder()
                .legs(List.of(
                        FastestPathResponse.RouteLeg.builder()
                                .startStation("역삼").endStation("강남").lineName("2호선").direction("내선").sectionTime(120).build(),
                        FastestPathResponse.RouteLeg.builder()
                                .startStation("강남").endStation("신사").lineName("신분당선").direction("상행").sectionTime(180).build()
                ))
                .build();

        // 2. Mock Station 객체
        Station yeoksam = Station.builder().stationCode("221").stationName("역삼").lineName("2호선").build();
        Station gangnam2 = Station.builder().stationCode("222").stationName("강남").lineName("2호선").build();
        Station gangnamSin = Station.builder().stationCode("D07").stationName("강남").lineName("신분당선").build();
        Station sinsa = Station.builder().stationCode("D04").stationName("신사").lineName("신분당선").build();

        // 3. Mock Repository 동작 정의
        // 역 조회
        when(stationRepository.findStationByStationNameAndLineName("역삼역", "2호선")).thenReturn(Optional.of(yeoksam));
        when(stationRepository.findStationByStationNameAndLineName("강남역", "2호선")).thenReturn(Optional.of(gangnam2));
        when(stationRepository.findStationByStationNameAndLineName("강남역", "신분당선")).thenReturn(Optional.of(gangnamSin));
        when(stationRepository.findStationByStationNameAndLineName("신사역", "신분당선")).thenReturn(Optional.of(sinsa));

        // 빠른 하차/환승 정보 조회
        // 역삼 -> 강남(환승) : Transfer 조회 (강남2호선 -> 강남신분당선)
        when(transferRepository.findByFromStationAndToStation(gangnam2, gangnamSin))
                .thenReturn(Optional.of(Transfer.builder().carNumber(1).doorNumber(1).build()));
        
        // 강남 -> 신사(도착) : Exit 조회 (신사신분당선)
        when(exitRepository.findByStationAndDirection(eq(sinsa), anyInt()))
                .thenReturn(List.of(Exit.builder().carNumber(2).doorNumber(2).build()));

        // 시간표 조회 (Mocking TimeTable)
        TimeTable mockTimeTable = TimeTable.builder().leftTime("12:00:00").build();
        when(timeTableRepository.findNextTrains(anyString(), anyString(), anyString(), anyString(), any(Pageable.class)))
                .thenReturn(List.of(mockTimeTable));

        // When
        pythonPathService.triggerPythonPathCalculation(response);

        // Then
        // PythonApiClient가 호출되었는지 검증하고, 파라미터 캡처
        ArgumentCaptor<PythonPathRequest> captor = ArgumentCaptor.forClass(PythonPathRequest.class);
        verify(pythonApiClient).requestPathCalculation(captor.capture());

        PythonPathRequest request = captor.getValue();

        // 검증
        assertEquals("221", request.getStartStationId()); // 역삼
        assertEquals("D04", request.getEndStationId());   // 신사
        
        // 환승역 리스트 검증
        assertEquals(1, request.getTransferStationIds().size());
        assertEquals("222", request.getTransferStationIds().get(0)); // 강남(2호선)에서 내림

        // 빠른 탑승 위치 검증
        // 출발 시 (역삼->강남 환승): Transfer 정보 (1-1)
        assertEquals(1, request.getStartFastBoarding().get(0)); 
        
        // 환승 후 (강남->신사 도착): Exit 정보 (2-2)
        assertEquals(2, request.getTransferFastBoardings().get(0).get(0));
    }
}
