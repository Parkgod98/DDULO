package com.apitest.ddulo.domain.station.dto.external.openapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;
/*
* 국토교통부_철도역 빠른환승 정보 API 호출 결과를 담아오는 DTO
* - 갈아탈 노선으로 가장 빠르게 이동할 수 있는 차량순서, 차량출입문번호 정보를 포함
*
* */

@Getter
@NoArgsConstructor
@ToString
public class FastTransferData {

    private int currentCount;
    private int matchCount;
    private int page;
    private int perPage;
    private int totalCount;

    private List<Data> data;

    @Getter
    @NoArgsConstructor
    @ToString
    public static class Data {
        @JsonProperty("노선명")
        private String lineName;

        @JsonProperty("역명")
        private String stationName;

        @JsonProperty("종착역명")
        private String terminalStationName;

        @JsonProperty("차량순서")
        private Integer carNumber;

        @JsonProperty("차량출입문번호")
        private Integer doorNumber;

        @JsonProperty("철도운영기관명")
        private String operator; // 필터링 대상 1

        @JsonProperty("환승기점역명")
        private String transferStartStation; // 보통 null이거나 안 씀

        @JsonProperty("환승선")
        private String transferLineName; // 필터링 대상 2

        @JsonProperty("환승이후역명")
        private String transferStationName;

        @JsonProperty("환승철도운영기관명")
        private String transferOperator; // 필터링 대상 3
    }
}