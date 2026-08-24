package com.apitest.ddulo.domain.path.dto.external.redis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PathPredictionData {

    private PathContext context;    //응답이 잘 됐는지 확인하는 용도
    private long totalTimeSec;      //총 소요시간(초)
    private List<PathResult> results;   //경로 결과 리스트

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PathContext {
        private String searchDate;  //조회일(오늘날짜)
        private String dayOfWeek;   //요일(ex: WED)
        private String searchTime;  //시간대(ex: 1800)
        private String redisKey;    //레디스 키(ex: path:pred:221:748:WED:1800)
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PathResult {
        private List<RouteStation> route;   // 경로 흐름 (출발 -> 환승 -> 도착 역 정보)

        // 추천 시간표 및 탑승 확률 (ex: 현재 가장 빨리 도착하는 열차, 탈 수 있는 열차)
        private List<ScheduleOption> schedule;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteStation {
        private String stationId;      //역 ID(역코드)
        private String lineName;    //노선명
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleOption {
        private String departureTime; // 출발 시각
        private String arrivalTime;   // 도착 시각
        private int waitingSec;       // 예상 대기 시간(초)
        private int boardingProbability;    //예상 탑승 확률(정수)
    }
}
