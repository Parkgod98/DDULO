package com.apitest.ddulo.domain.subway.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class TrainCongestionDto {
    private LocalTime arrivalTime;
    private List<Integer> carCongestionPercentiles;
}

