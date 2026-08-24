package com.apitest.ddulo.domain.path.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PythonPathRequest {
    @JsonProperty("day_of_week")
    private String dayOfWeek;

    @JsonProperty("start_station_id")
    private String startStationId;

    @JsonProperty("start_direction")
    private Integer startDirection;

    @JsonProperty("start_time")
    private List<String> startTime;

    @JsonProperty("start_fast_boarding")
    private List<Integer> startFastBoarding; // [칸, 문]

    @JsonProperty("transfer_station_ids")
    private List<String> transferStationIds;

    @JsonProperty("transfer_directions")
    private List<Integer> transferDirections;

    @JsonProperty("transfer_times")
    private List<List<String>> transferTimes;

    @JsonProperty("transfer_fast_boardings")
    private List<List<Integer>> transferFastBoardings; // [[칸, 문], ...]

    @JsonProperty("end_station_id")
    private String endStationId;
}
