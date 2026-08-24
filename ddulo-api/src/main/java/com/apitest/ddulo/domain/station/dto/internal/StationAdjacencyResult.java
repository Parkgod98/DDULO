package com.apitest.ddulo.domain.station.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationAdjacencyResult {
    private List<StationNode> prevStations; // 이전 역 목록 (행선지 기준 상행/시점 방향)
    private List<StationNode> nextStations; // 다음 역 목록 (행선지 기준 하행/종점 방향)
}
