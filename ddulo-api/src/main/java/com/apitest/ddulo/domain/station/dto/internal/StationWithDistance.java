package com.apitest.ddulo.domain.station.dto.internal;

import com.apitest.ddulo.domain.station.domain.Station;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 역 정보와 계산된 거리 정보를 한데 묶어 관리하는 내부용 DTO
 */
@Getter
@RequiredArgsConstructor
public class StationWithDistance {
    private final Station station;
    private final int distance;
}
