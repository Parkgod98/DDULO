package com.apitest.ddulo.domain.station.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "최단시간 경로 DTO")
public class FastestPathResponse {
    @Schema(description = "총 소요시간(초)", example = "2240")
    private int totalTime;          // 총 소요 시간 (초)
    @Schema(description = "총 환승횟수", example = "1")
    private int transferCount;      // 환승 횟수
    @Schema(description = "구간별 상세 정보")
    private List<RouteLeg> legs;    // 구간별 상세 정보

    @Getter
    @Builder
    @Schema(description = "구간별 상세 정보")
    public static class RouteLeg {
        @Schema(description = "구간 출발역", example = "역삼")
        private String startStation; // 구간 출발역
        @Schema(description = "구간 도착역", example = "대림")
        private String endStation;   // 구간 도착역
        @Schema(description = "이용 노선", example = "2호선")
        private String lineName;     // 이용 노선
        @Schema(description = "방향", example = "내선")
        private String direction;    // 상행(내선)/하행(외선) 구분
        @Schema(description = "구간 소요시간", example = "1080")
        private int sectionTime;     // 구간 소요 시간
    }
}