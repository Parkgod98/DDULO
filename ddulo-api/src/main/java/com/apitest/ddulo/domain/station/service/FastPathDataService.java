package com.apitest.ddulo.domain.station.service;

import com.apitest.ddulo.domain.station.client.RouteApiClient;
import com.apitest.ddulo.domain.station.dto.external.openapi.FastPathData;
import com.apitest.ddulo.domain.station.dto.response.FastestPathResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.apitest.ddulo.global.utils.StationUtils.addStationSuffix;
import static com.apitest.ddulo.global.utils.StationUtils.removeStationSuffix;

@Slf4j
@Service
@RequiredArgsConstructor
public class FastPathDataService {

    private final RouteApiClient routeApiClient;

    public FastestPathResponse getSubwayRoute(String startStation, String endStation) {
        // 외부 API 데이터 조회
        FastPathData rawData = routeApiClient.searchRoute(removeStationSuffix(startStation), removeStationSuffix(endStation));

        // 데이터 검증 (Null Check)
        if (rawData == null || rawData.getBody() == null || rawData.getBody().getPaths() == null) {
            log.warn("경로 데이터 없음: {} -> {}", startStation, endStation);
            return FastestPathResponse.builder()
                    .totalTime(0)
                    .transferCount(0)
                    .legs(Collections.emptyList())
                    .build();
        }

        // 데이터 가공 (Raw Data -> Clean Response)
        return transformToRouteResponse(rawData);
    }

    // 데이터 가공 로직 분리 (가독성 향상)
    private FastestPathResponse transformToRouteResponse(FastPathData rawData) {
        List<FastPathData.Path> paths = rawData.getBody().getPaths();
        List<FastestPathResponse.RouteLeg> legs = new ArrayList<>();

        String currentLine = null;
        String startStation = null;
        String currentDirection = null; // 방향 정보 추가
        int sectionTimeAccumulator = 0;

        for (FastPathData.Path segment : paths) {
            String depName = segment.getDepartureStation().getStationName();
            String arrName = segment.getArrivalStation().getStationName();
            String lineName = segment.getDepartureStation().getLineName();
            String direction = segment.getDirection(); // 방향 정보 가져오기

            // Case A: 환승 보행 구간 (출발역 == 도착역)
            if (depName.equals(arrName)) {
                if (currentLine != null) {
                    addLeg(legs, startStation, depName, currentLine, currentDirection, sectionTimeAccumulator);
                }
                // 리셋
                currentLine = null;
                currentDirection = null;
                sectionTimeAccumulator = segment.getRequiredTime(); // 환승 시간 포함
                startStation = arrName;
                continue;
            }

            // Case B: 지하철 탑승 구간 시작
            if (currentLine == null) {
                currentLine = lineName;
                currentDirection = direction;
                if (startStation == null) startStation = depName;
            }

            // 시간 누적
            sectionTimeAccumulator += segment.getRequiredTime();
        }

        // Case C: 마지막 구간 처리
        if (currentLine != null && !paths.isEmpty()) {
            String lastStation = paths.get(paths.size() - 1).getArrivalStation().getStationName();
            addLeg(legs, startStation, lastStation, currentLine, currentDirection, sectionTimeAccumulator);
        }

        return FastestPathResponse.builder()
                .totalTime(rawData.getBody().getTotalTime())
                .transferCount(rawData.getBody().getTransferCount())
                .legs(legs)
                .build();
    }

    // 리스트 추가 헬퍼 메서드
    private void addLeg(List<FastestPathResponse.RouteLeg> legs, String start, String end, String line, String direction, int time) {
        legs.add(FastestPathResponse.RouteLeg.builder()
                .startStation(addStationSuffix(start))
                .endStation(addStationSuffix(end))
                .lineName(line)
                .direction(direction)
                .sectionTime(time)
                .build());
    }
}
