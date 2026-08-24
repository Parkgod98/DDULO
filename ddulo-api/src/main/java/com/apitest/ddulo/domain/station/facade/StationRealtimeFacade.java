package com.apitest.ddulo.domain.station.facade;

import com.apitest.ddulo.domain.station.dto.internal.StationAdjacencyResult;
import com.apitest.ddulo.domain.station.dto.response.StationArrivalResponse;
import com.apitest.ddulo.domain.station.dto.external.redis.StationDetailData;
import com.apitest.ddulo.domain.station.service.PythonStationService;
import com.apitest.ddulo.domain.station.service.StationAdjacencyService;
import com.apitest.ddulo.domain.station.service.StationRedisService;
import com.apitest.ddulo.domain.station.service.StationResponseService;
import com.apitest.ddulo.global.exception.CustomException;
import com.apitest.ddulo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StationRealtimeFacade {

    private final StationRedisService stationRedisService;
    private final StationAdjacencyService stationAdjacencyService;
    private final PythonStationService pythonStationService;
    private final StationResponseService stationResponseService;

    @Transactional(readOnly = true)
    public StationArrivalResponse getRealtimeStationDetail(String stationCode) {
        // 없는 역코드면 예외처리
        stationAdjacencyService.validateStationExists(stationCode);

        // Python API 호출 (Redis 적재 트리거)
        pythonStationService.triggerPythonCalculation(stationCode);

        // redis에서 실시간 역/열차 정보 조회
        StationDetailData stationDetailData = stationRedisService.getStationDetail(stationCode);

        // 인접 역 리스트 조회
        StationAdjacencyResult nearStations = stationAdjacencyService.getAdjacentStations(stationCode);
        // 인접 역이 모두 없는 경우 예외처리
        if(nearStations.getPrevStations().isEmpty() && nearStations.getNextStations().isEmpty())
            throw new CustomException(ErrorCode.DATA_NOT_FOUND);

        return stationResponseService.buildStationArrivalResponse(stationDetailData, nearStations);
    }
}
