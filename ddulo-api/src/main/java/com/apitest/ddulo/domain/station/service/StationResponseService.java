package com.apitest.ddulo.domain.station.service;

import com.apitest.ddulo.domain.station.domain.Station;
import com.apitest.ddulo.domain.station.dto.internal.StationAdjacencyResult;
import com.apitest.ddulo.domain.station.dto.response.StationArrivalResponse;
import com.apitest.ddulo.domain.station.dto.external.redis.StationDetailData;
import com.apitest.ddulo.domain.station.repository.StationRepository;
import com.apitest.ddulo.global.exception.CustomException;
import com.apitest.ddulo.global.exception.ErrorCode;
import com.apitest.ddulo.global.utils.SubwayUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StationResponseService {

    private final StationRepository stationRepository;

    public StationArrivalResponse buildStationArrivalResponse(
            StationDetailData stationDetailData,
            StationAdjacencyResult nearStations) {

        String stationCode = stationDetailData.getStation().getStationId();

        // DB에서 역 정보 조회하여 이름과 노선명 채우기
        Station stationEntity = stationRepository.findByStationCode(stationCode)
                .orElseThrow(() -> new CustomException(ErrorCode.DATA_NOT_FOUND));

        String lineName = stationEntity.getLineName();

        return StationArrivalResponse.builder()
                .station(StationArrivalResponse.StationInfo.builder()
                        .stationCode(stationCode)
                        .stationName(stationEntity.getStationName())
                        .lineName(lineName)
                        .prevStations(nearStations.getPrevStations())
                        .nextStations(nearStations.getNextStations())
                        .build())
                .upBound(stationDetailData.getUpBound().stream()
                        .map(arrival -> mapToArrivalInfo(arrival, lineName))
                        .toList())
                .downBound(stationDetailData.getDownBound().stream()
                        .map(arrival -> mapToArrivalInfo(arrival, lineName))
                        .toList())
                .build();
    }

    private StationArrivalResponse.ArrivalInfo mapToArrivalInfo(
            StationDetailData.TrainArrival arrival,
            String lineName) {

        return StationArrivalResponse.ArrivalInfo.builder()
                .direction(SubwayUtils.getDirectionName(arrival.getDirection(), lineName))
                .arrivalSec(arrival.getArrivalSec())
                .currentStation(arrival.getCurrentStation())
                .destination(arrival.getDestination())
                .isBoardable(arrival.isBoardable())

                .carCongestions(arrival.getCarCongestions().stream()
                        .map(congestion -> StationArrivalResponse.CarCongestion.builder()
                                .carNo(congestion.getCarNo())
                                .congestionLevel(congestion.getCongestionLevel())
                                .build())
                        .toList())
                .build();
    }
}
