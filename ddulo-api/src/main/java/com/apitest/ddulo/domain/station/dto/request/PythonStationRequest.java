package com.apitest.ddulo.domain.station.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PythonStationRequest {
    @JsonProperty("day_of_week")
    private String dayOfWeek;

    @JsonProperty("station_id")
    private String stationId;

    @JsonProperty("left_times")
    private List<String> leftTimes;

    @JsonProperty("right_times")
    private List<String> rightTimes;
}
