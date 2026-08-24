package com.apitest.ddulo.domain.station.controller;

import com.apitest.ddulo.domain.station.dto.response.StationArrivalResponse;
import com.apitest.ddulo.domain.station.facade.StationRealtimeFacade;
import com.apitest.ddulo.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/station-detail")
@RequiredArgsConstructor
@Tag(name = "지하철역 상세정보 API", description = "역 상세정보를 조회할 수 있는 REST API")
public class StationDetailController {

    private final StationRealtimeFacade stationRealtimeFacade;

    @GetMapping("/{station_code}")
    @Operation(summary = "역 상세정보 조회", description = "역 코드를 통해 해당 역의 상세정보를 조회할 수 있다.")
    public ApiResponse<StationArrivalResponse> stationDetailInfo(
            @PathVariable("station_code") @Parameter(description = "역 코드", example = "221") String stationCode) {
        return ApiResponse.success(stationRealtimeFacade.getRealtimeStationDetail(stationCode));
    }
}
