package com.apitest.ddulo.domain.station.dto.response;

import com.apitest.ddulo.domain.station.dto.internal.StationNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "역 상세정보 DTO")
public class StationArrivalResponse {
    @Schema(description = "역 기본정보")
    private StationInfo station;    // 역 기본정보
    @Schema(description = "인접 상행/내선 열차 실시간 데이터 리스트")
    private List<ArrivalInfo> upBound;   // 상행/내선 (최대 3개)
    @Schema(description = "인접 하행/외선 열차 실시간 데이터 리스트")
    private List<ArrivalInfo> downBound; // 하행/외선 (최대 3개)

    @Getter
    @Builder
    @Schema(description = "역 기본정보")
    public static class StationInfo {
        @Schema(description = "역 코드", example = "221")
        private String stationCode; // 역 ID(pk)
        @Schema(description = "역 이름", example = "역삼역")
        private String stationName;     // 역명
        @Schema(description = "노선명", example = "2호선")
        private String lineName;    // 노선명
        @Schema(description = "이전 역 명", example = "선릉역")
        private List<StationNode> prevStations;     //이전 역 명
        @Schema(description = "다음 역 명", example = "강남역")
        private List<StationNode> nextStations;     //다음 역 명
    }

    @Getter
    @Builder
    @Schema(description = "열차 실시간 데이터 리스트")
    public static class ArrivalInfo {
        @Schema(description = "진행방향", example = "성수(내선)행")
        private String direction;    // 방면 (예: 성수행)
        @Schema(description = "도착까지 남은 시간(초)", example = "150")
        private Integer arrivalSec;   // 도착까지 남은 시간(초)
        @Schema(description = "현재 열차 위치", example = "선릉")
        private String currentStation; // 현재 열차 위치
        @Schema(description = "종착역", example = "성수역")
        private String destination;  // 종착역
        @Schema(description = "탑승 가능여부", example = "true")
        private Boolean isBoardable; // 탑승 가능여부

        @Schema(description = "열차 칸별 혼잡도")
        private List<CarCongestion> carCongestions; //열차 칸별 혼잡도
    }

    @Getter
    @Builder
    @Schema(description = "열차 칸별 혼잡도")
    public static class CarCongestion {
        @Schema(description = "객차 번호", example = "1")
        private int carNo;      // 객차 번호(1~10)
        @Schema(description = "혼잡도 수치", example = "33")
        private int congestionLevel;    // 혼잡도 수치
    }
}
