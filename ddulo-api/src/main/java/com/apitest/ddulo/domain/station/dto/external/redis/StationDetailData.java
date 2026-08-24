package com.apitest.ddulo.domain.station.dto.external.redis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class StationDetailData {

    private StationContext context; //응답이 잘 됐는지 확인하는 용도
    private StationInfo station;    //역 정보
    private List<TrainArrival> upBound;  // 상행/내선 열차 목록
    private List<TrainArrival> downBound;  // 하행/외선 열차 목록

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StationContext {
        private String targetDate;  //조회일(오늘날짜)
        private String dayOfWeek;   //요일(ex: WED)
        private String timeRange;   //시간대(ex: 1800)
        private String redisKey;    //레디스 키(ex: stat:near:221:WED:1430)
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StationInfo {
        private String stationId;   //역 ID(역 코드임)
        private String stationName; //역 이름
        private String lineName;    //노선명
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrainArrival {
        private int direction;       // 운행방향(0:상행/외선, 1:하행/내선)
        private int arrivalSec;      // 도착까지 남은 시간
        private String currentStation; // 현재 열차 위치 (예: 선릉)
        private String destination;    // 행선지 (예: 신도림)

        // JSON 키는 "isBoardable"이지만, 자바 필드명은 boardable로 하고 매핑을 명시
        // (Lombok getter 생성 시 혼동 방지)
        @JsonProperty("isBoardable")
        private boolean boardable;  //탑승가능 여부

        private List<CarCongestion> carCongestions;   // 칸별 혼잡도 리스트
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CarCongestion {
        private int carNo;      //객차번호
        private int congestionLevel;    //혼잡도
    }
}
