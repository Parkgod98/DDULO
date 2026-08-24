package com.apitest.ddulo.domain.station.dto.external.openapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@NoArgsConstructor
@ToString
public class FastPathData {

    @JsonProperty("body")
    private Body body;

    @Getter
    @NoArgsConstructor
    @ToString
    public static class Body {
        @JsonProperty("totalreqHr")
        private int totalTime;      // 총 소요시간

        @JsonProperty("trsitNmtm")
        private int transferCount;  // 환승 횟수

        @JsonProperty("paths")
        private List<Path> paths;
    }

    @Getter
    @NoArgsConstructor
    @ToString
    public static class Path {
        @JsonProperty("reqHr")
        private int requiredTime;   // 구간 소요 시간

        @JsonProperty("dptreStn")
        private StationInfo departureStation; // 출발역 정보

        @JsonProperty("arvlStn")
        private StationInfo arrivalStation;   // 도착역 정보

        @JsonProperty("upbdnbSe")
        private String direction;   // 상행(내선)/하행(외선) 구분
    }

    @Getter
    @NoArgsConstructor
    @ToString
    public static class StationInfo {
        @JsonProperty("stnNm")
        private String stationName; // 역명

        @JsonProperty("lineNm")
        private String lineName;    // 호선명
    }
}