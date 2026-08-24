package com.apitest.ddulo.domain.subway.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class StatDto {

    private String startStationCode;
    private String startStationName;

    private String endStationCode;
    private String endStationName;

    private String prevStationCode;
    private String prevStationName;

    private int updnLine;   // 방향
    private int directAt;   // 급행 여부

    private List<CongestionDataDto> data;
}

