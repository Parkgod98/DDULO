package com.apitest.ddulo.domain.station.facade;

import com.apitest.ddulo.domain.path.dto.response.PathPredictionResponse;
import com.apitest.ddulo.domain.path.dto.response.ResultResponse;
import com.apitest.ddulo.domain.path.service.PathPredictionService;
import com.apitest.ddulo.domain.path.service.PythonPathService;
import com.apitest.ddulo.domain.path.service.ResultService;
import com.apitest.ddulo.domain.station.dto.response.FastestPathResponse;
import com.apitest.ddulo.domain.station.dto.response.StationTotalResponse;
import com.apitest.ddulo.domain.station.service.FastPathDataService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StationRouteFacadeTest {

    @InjectMocks
    private StationRouteFacade stationRouteFacade;

    @Mock
    private FastPathDataService fastPathDataService;
    @Mock
    private PathPredictionService pathPredictionService;
    @Mock
    private ResultService resultService;
    @Mock
    private PythonPathService pythonPathService;

    @Test
    @DisplayName("경로 조회 Facade - 정상 케이스")
    void getStationRoute_Success() {
        // Given
        FastestPathResponse fastestPathResponse = FastestPathResponse.builder().build();
        PathPredictionResponse pathPredictionResponse = PathPredictionResponse.builder().build();
        ResultResponse resultResponse = ResultResponse.builder().build();

        when(fastPathDataService.getSubwayRoute(anyString(), anyString())).thenReturn(fastestPathResponse);
        when(pathPredictionService.predictPathDetails(any(FastestPathResponse.class))).thenReturn(pathPredictionResponse);
        when(resultService.getResult(any(FastestPathResponse.class), any(PathPredictionResponse.class))).thenReturn(resultResponse);

        // When
        StationTotalResponse response = stationRouteFacade.getStationRoute("역삼", "강남");

        // Then
        assertNotNull(response);
        assertNotNull(response.getFastestPathResponse());
        assertNotNull(response.getPathPredictionResponse());
        assertNotNull(response.getResultResponse());

        // Python API 호출 트리거 검증
        verify(pythonPathService).triggerPythonPathCalculation(fastestPathResponse);
    }
}
