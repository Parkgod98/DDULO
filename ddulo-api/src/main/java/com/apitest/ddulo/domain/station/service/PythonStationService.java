package com.apitest.ddulo.domain.station.service;

import com.apitest.ddulo.domain.station.dto.request.PythonStationRequest;
import com.apitest.ddulo.domain.station.client.PythonApiClient;
import com.apitest.ddulo.domain.timetable.domain.TimeTable;
import com.apitest.ddulo.domain.timetable.repository.TimeTableRepository;
import com.apitest.ddulo.global.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PythonStationService {

    private final PythonApiClient pythonApiClient;
    private final TimeTableRepository timeTableRepository;

    @Transactional(readOnly = true)
    public void triggerPythonCalculation(String stationCode) {
        try {
            String nowTimeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String dayKey = DateUtils.convertDateToDayKey(String.valueOf(LocalDate.now()));
            String weekTag = DateUtils.convertDayKeyToWeekTag(dayKey);

            // 상행(1) 3개 조회 -> 반올림 변환
            List<String> leftTimes = timeTableRepository.findNextTrains(
                    stationCode, weekTag, "0", nowTimeStr, PageRequest.of(0, 3))
                    .stream()
                    .map(TimeTable::getLeftTime)
                    .map(DateUtils::roundToNearest10Minutes)
                    .toList();

            // 하행(2) 3개 조회 -> 반올림 변환
            List<String> rightTimes = timeTableRepository.findNextTrains(
                    stationCode, weekTag, "1", nowTimeStr, PageRequest.of(0, 3))
                    .stream()
                    .map(TimeTable::getLeftTime)
                    .map(DateUtils::roundToNearest10Minutes)
                    .toList();

            PythonStationRequest request = PythonStationRequest.builder()
                    .dayOfWeek(dayKey)
                    .stationId(stationCode)
                    .leftTimes(leftTimes)
                    .rightTimes(rightTimes)
                    .build();

            System.out.println(request.toString());

            pythonApiClient.requestStationCalculation(request);

        } catch (Exception e) {
            log.error("Failed to trigger python calculation for station {}: {}", stationCode, e.getMessage());
        }
    }
}
