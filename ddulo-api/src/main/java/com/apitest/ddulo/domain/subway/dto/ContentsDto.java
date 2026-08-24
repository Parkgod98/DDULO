package com.apitest.ddulo.domain.subway.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class ContentsDto {

    private String subwayLine;
    private String stationName;
    private String stationCode;

    private List<StatDto> stat;

    private String statStartDate;
    private String statEndDate;
}

