package com.apitest.ddulo.domain.subway.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class StationDetailResponseDto {
    private Long stationId;
    private String stationName;
    private List<DirectionCongestionDto> directions;
}

