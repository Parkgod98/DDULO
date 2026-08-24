package com.apitest.ddulo.domain.station.service;

import com.apitest.ddulo.domain.station.client.PythonApiClient;
import com.apitest.ddulo.domain.station.dto.request.PythonStationRequest;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PythonStationServiceTest {

    @InjectMocks
    private PythonStationService pythonStationService;

    @Mock
    private PythonApiClient pythonApiClient;
    @Mock
    private TimeTableRepository timeTableRepository;

    @Test
    @DisplayName("Python API 호출 트리거 테스트")
    void triggerPythonCalculation_Success() {
        // Given
        TimeTable t1 = TimeTable.builder().leftTime("12:00:00").build();
        when(timeTableRepository.findNextTrains(anyString(), anyString(), anyString(), anyString(), any(Pageable.class)))
                .thenReturn(List.of(t1));

        // When
        pythonStationService.triggerPythonCalculation("221");

        // Then
        ArgumentCaptor<PythonStationRequest> captor = ArgumentCaptor.forClass(PythonStationRequest.class);
        verify(pythonApiClient).requestStationCalculation(captor.capture());
        
        PythonStationRequest request = captor.getValue();
        assertEquals("221", request.getStationId());
    }
}
