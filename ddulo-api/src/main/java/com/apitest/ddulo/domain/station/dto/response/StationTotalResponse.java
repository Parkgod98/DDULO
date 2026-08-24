package com.apitest.ddulo.domain.station.dto.response;

import com.apitest.ddulo.domain.path.dto.response.PathPredictionResponse;
import com.apitest.ddulo.domain.path.dto.response.ResultResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StationTotalResponse {
    private FastestPathResponse fastestPathResponse;
    private PathPredictionResponse pathPredictionResponse;
    private ResultResponse resultResponse;

}
