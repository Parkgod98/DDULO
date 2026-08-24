package com.apitest.ddulo.domain.station.facade;

import com.apitest.ddulo.domain.path.dto.response.PathPredictionResponse;
import com.apitest.ddulo.domain.path.dto.response.ResultResponse;
import com.apitest.ddulo.domain.path.service.PathPredictionService;
import com.apitest.ddulo.domain.path.service.PythonPathService;
import com.apitest.ddulo.domain.path.service.ResultService;
import com.apitest.ddulo.domain.station.dto.response.FastestPathResponse;
import com.apitest.ddulo.domain.station.dto.response.StationTotalResponse;
import com.apitest.ddulo.domain.station.service.FastPathDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StationRouteFacade {

    private final FastPathDataService fastPathDataService;
    private final PathPredictionService pathPredictionService;
    private final ResultService resultService;
    private final PythonPathService pythonPathService;

    @Transactional(readOnly = true)
    public StationTotalResponse getStationRoute(String startStationName, String endStationName) {
        log.info("경로 조회 시작 - 출발역: {}, 도착역: {}", startStationName, endStationName);

        try {
            // 최단 경로 조회
            FastestPathResponse fastestPathResponse = fastPathDataService.getSubwayRoute(startStationName, endStationName);
            log.debug("최단 경로 조회 완료: {}", fastestPathResponse);

            // Python API 호출 (Redis 적재 트리거)
            pythonPathService.triggerPythonPathCalculation(fastestPathResponse);

            // 경로 예측 정보 조회
            PathPredictionResponse pathPredictionResponse = pathPredictionService.predictPathDetails(
                    fastestPathResponse
            );
            log.debug("경로 예측 정보 조회 완료: {}", pathPredictionResponse);

            // 최종 결과 생성
            ResultResponse resultResponse = resultService.getResult(
                    fastestPathResponse,
                    pathPredictionResponse
            );
            log.debug("최종 결과 생성 완료: {}", resultResponse);

            // 종합 응답 구성
            StationTotalResponse response = StationTotalResponse.builder()
                    .fastestPathResponse(fastestPathResponse)
                    .pathPredictionResponse(pathPredictionResponse)
                    .resultResponse(resultResponse)
                    .build();

            log.info("경로 조회 완료 - 출발역: {}, 도착역: {}", startStationName, endStationName);
            return response;
        } catch (Exception e) {
            log.error("경로 조회 실패 - 출발역: {}, 도착역: {}", startStationName, endStationName, e);
            throw e;
        }
    }
}
