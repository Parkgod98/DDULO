package com.apitest.ddulo.domain.station.service;

import com.apitest.ddulo.domain.station.domain.Station;
import com.apitest.ddulo.domain.station.dto.internal.StationWithDistance;
import com.apitest.ddulo.domain.station.dto.response.HomeInitResponse;
import com.apitest.ddulo.domain.station.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.apitest.ddulo.global.utils.StationUtils.calculateDistance;

@Slf4j
@Service
@RequiredArgsConstructor
public class StationInitService {

    private final StationRepository stationRepository;

    // 앱 초기 실행 시 호출되는 메서드
    // 전체 역 데이터 + 내 주변(1km) 역 데이터를 한 번에 반환
    @Transactional(readOnly = true)
    public HomeInitResponse getHomeData(Double userLat, Double userLon) {

        // DB에서 모든 역 데이터 가져오기 (쿼리 1회 발생), 캐싱
        List<Station> allStations = getAllStationsCached();

        // 전체 역 리스트 변환 (거리 정보 없음)
        List<HomeInitResponse.StationDto> allStationDtos = allStations.stream()
                .map(station -> HomeInitResponse.StationDto.fromEntity(station, null))
                .collect(Collectors.toList());

        // 내 주변 1km 이내 역 필터링 및 거리 계산
        List<HomeInitResponse.StationDto> nearbyStationDtos;

        if(userLat == null || userLon == null) {
            nearbyStationDtos = Collections.emptyList();
        } else {
            nearbyStationDtos = allStations.stream()
                    // 위경도 데이터가 없는 역은 계산에서 제외 (NPE 방지)
                    .filter(station -> station.getLatitude() != null && station.getLongitude() != null)
                    .map(station -> {
                        // 거리 계산 (미터 단위)
            int distance = calculateDistance(userLat, userLon, station.getLatitude(), station.getLongitude());
            return new StationWithDistance(station, distance); // 임시 객체로 매핑
        })
                    .filter(dto -> dto.getDistance() <= 1000) // 1km(1000m) 이내 필터링
                    .sorted(Comparator.comparingInt(dto -> dto.getDistance())) // 가까운 순 정렬
//                .limit(5) // 너무 많으면 상위 N개만 출력
                    .map(dto -> HomeInitResponse.StationDto.fromEntity(dto.getStation(), dto.getDistance()))
                    .collect(Collectors.toList());
        }

        // 결과 조립 및 반환
        return HomeInitResponse.builder()
                    .nearbyStations(nearbyStationDtos)
                    .allStations(allStationDtos)
                    .build();
    }

    // 호출한 데이터를 메모리에 캐싱
    @Cacheable(value = "stations")
    public List<Station> getAllStationsCached() {
        return stationRepository.findAll();
    }
}