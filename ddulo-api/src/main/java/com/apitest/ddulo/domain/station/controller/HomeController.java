package com.apitest.ddulo.domain.station.controller;

import com.apitest.ddulo.domain.station.dto.response.HomeInitResponse;
import com.apitest.ddulo.domain.station.service.StationInitService;
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
@RequestMapping("/api/v1/load")
@RequiredArgsConstructor
@Tag(name = "앱 실행 시 필요한 데이터 요청 API", description = "앱 시작 시 필요한 데이터 요청에 대한 REST API")
public class HomeController {

    private final StationInitService stationInitService;

    @GetMapping
    @Operation(summary = "사용자의 위치반경 내 가까운 역을 조회", description = "사용자의 위경도를 받아서 근처(반경 1km 내)에 있는 역을 조회할 수 있다.")
    public ApiResponse<HomeInitResponse> initializeApp(
            @RequestParam(required = false) @Parameter(description = "사용자의 위도") Double lat,
            @RequestParam(required = false) @Parameter(description = "사용자의 경도") Double lon) {
        return ApiResponse.success(stationInitService.getHomeData(lat, lon));
    }
}
