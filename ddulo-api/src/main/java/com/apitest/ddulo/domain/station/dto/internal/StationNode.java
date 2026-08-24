package com.apitest.ddulo.domain.station.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationNode {
    private String stationCode; // 역코드 (ex: "222", "P144-1")
    private String stationName; // 역이름 (ex: "강남역")
}
