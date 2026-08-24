package com.apitest.ddulo.domain.subway.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class DirectionCongestionDto {
    private int direction; // 0: 상행, 1: 하행
    private List<TrainCongestionDto> trains;
}

