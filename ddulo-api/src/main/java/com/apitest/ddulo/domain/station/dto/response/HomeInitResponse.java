package com.apitest.ddulo.domain.station.dto.response;

import com.apitest.ddulo.domain.station.domain.Station;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "사용자 위치반경 내 역 데이터 DTO")
public class HomeInitResponse {
    @Schema(description = "역 목록")
    private List<StationDto> nearbyStations;  // 내 주변 1km 이내 역 (추천용, 상단 노출)
    @Schema(description = "전체 역 목록")
    private List<StationDto> allStations;   // 전체 역 리스트 (검색용, 로컬 DB 캐싱용)

    @Getter
    @Builder
    public static class StationDto {
        @Schema(description = "역 ID", example = "221")
        private Long stationId;
        @Schema(description = "역 이름", example = "역삼역")
        private String stationName;
        @Schema(description = "노선명", example = "2호선")
        private String lineName;
        @Schema(description = "역의 위도", example = "37.500658")
        private Double latitude;
        @Schema(description = "역의 경도", example = "127.03643")
        private Double longitude;
        @Schema(description = "사용자와 역 사이의 거리", example = "500(all에서는 null)")
        private Integer distanceMeters;     // 거리 정보는 nearby 리스트에만 값이 있고, all에는 null일 수 있음

        public static StationDto fromEntity(Station station, Integer distance) {
            return StationDto.builder()
                    .stationId(station.getStationId())
                    .stationName(station.getStationName())
                    .lineName(station.getLineName())
                    .latitude(station.getLatitude())
                    .longitude(station.getLongitude())
                    .distanceMeters(distance)
                    .build();
        }
    }
}
