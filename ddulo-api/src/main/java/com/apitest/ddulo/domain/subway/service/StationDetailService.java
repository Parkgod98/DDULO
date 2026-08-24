package com.apitest.ddulo.domain.subway.service;

import com.apitest.ddulo.domain.station.domain.Station;
import com.apitest.ddulo.domain.station.repository.StationRepository;
import com.apitest.ddulo.domain.subway.domain.TrainCongestion;
import com.apitest.ddulo.domain.subway.dto.DirectionCongestionDto;
import com.apitest.ddulo.domain.subway.dto.StationDetailResponseDto;
import com.apitest.ddulo.domain.subway.dto.TrainCongestionDto;
import com.apitest.ddulo.domain.subway.repository.TrainCongestionRepository;
import com.apitest.ddulo.global.common.enums.DayOfWeek;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StationDetailService {

    private final StationRepository stationRepository;
    private final TrainCongestionRepository congestionRepository;
    private final ObjectMapper objectMapper;

    public StationDetailResponseDto getStationDetail(Long stationId) {

        // 1️⃣ 현재 시간 기준 hh / 요일
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek dayOfWeek = DayOfWeek.fromCode(LocalDate.now().getDayOfWeek().getValue());
        LocalTime currentTime = now.toLocalTime();

        // 2️⃣ stationId → stationCode
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new IllegalArgumentException("Station not found"));

        List<DirectionCongestionDto> directionResults = new ArrayList<>();

        // 3️⃣ 방향별 처리 (0: 상행, 1: 하행)
        for (int direction : List.of(0, 1)) {

            // 미래 시간 기준 2개 조회
            List<TrainCongestion> futureCongestions =
                    congestionRepository.findFutureCongestions(
                            station.getStationId(),
                            direction,
                            dayOfWeek,
                            currentTime,
                            PageRequest.of(0, 3)
                    );

            List<TrainCongestionDto> trains = futureCongestions.stream()
                    .map(this::toTrainDto)
                    .toList();

            directionResults.add(
                    new DirectionCongestionDto(direction, trains)
            );
        }

        return new StationDetailResponseDto(
                station.getStationId(),
                station.getStationName(),
                directionResults
        );
    }

    /**
     * 혼잡도 → 프론트 DTO 변환
     */
    private TrainCongestionDto toTrainDto(TrainCongestion congestion) {

        List<Integer> rawCars;
        try {
            rawCars = objectMapper.readValue(
                    congestion.getCongestionCarsJson(),
                    new TypeReference<>() {}
            );
        } catch (Exception e) {
            rawCars = List.of();
        }

        // 4️⃣ % → 백분위 변환
        List<Integer> percentileCars = rawCars.stream()
                .map(this::toPercentile)
                .toList();

        return new TrainCongestionDto(
                congestion.getMeasuredAt(),
                percentileCars
        );
    }

    /**
     * 300% 기준 백분위 변환
     */
    private int toPercentile(int congestionPercent) {
        return Math.min(100, (int) Math.round(congestionPercent / 300.0 * 100));
    }
}

