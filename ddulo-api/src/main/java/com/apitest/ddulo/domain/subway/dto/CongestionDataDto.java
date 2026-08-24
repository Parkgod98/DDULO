package com.apitest.ddulo.domain.subway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class CongestionDataDto {

    private String dow; // "MON", "TUE" ...
    private String hh;  // "08"
    private String mm;  // "00"

    private List<Integer> congestionCar;
}

