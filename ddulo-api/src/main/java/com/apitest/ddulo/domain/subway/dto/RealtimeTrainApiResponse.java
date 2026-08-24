package com.apitest.ddulo.domain.subway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class RealtimeTrainApiResponse {

    @JsonProperty("errorMessage")
    private ErrorMessage errorMessage;

    @JsonProperty("realtimePositionList")
    private List<RealtimeTrainRowDto> realtimePositionList;

    @Getter
    public static class ErrorMessage {
        private int status;
        private String code;
        private String message;
        private int total;
    }
}


