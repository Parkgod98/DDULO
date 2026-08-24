package com.apitest.ddulo.domain.station.controller;

import com.apitest.ddulo.domain.station.dto.response.FastestPathResponse;
import com.apitest.ddulo.domain.station.dto.response.StationTotalResponse;
import com.apitest.ddulo.domain.station.facade.StationRouteFacade;
import com.apitest.ddulo.domain.station.service.FastPathDataService;
import com.apitest.ddulo.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/route")
@RequiredArgsConstructor
@Tag(name = "최단시간 경로 API", description = "출발지에서 도착지까지의 최단시간 경로를 조회할 수 있는 REST API")
public class RouteController {

    private final FastPathDataService fastPathDataService;
    private final StationRouteFacade stationRouteFacade;

    @GetMapping
    @Operation(summary = "최단시간 경로 조회", description = "출발지에서 도착지까지의 최단시간 경로를 조회할 수 있다." +
            "또한, 해당 경로에 대한 종합 데이터를 반환한다.")
    public ApiResponse<StationTotalResponse> searchRoute(
            @RequestParam("from") @Parameter(description = "출발역", example = "역삼역") String fromStation,
            @RequestParam("to") @Parameter(description = "도착역", example = "광명사거리역") String toStation) {
        return ApiResponse.success(stationRouteFacade.getStationRoute(fromStation, toStation));
    }

}