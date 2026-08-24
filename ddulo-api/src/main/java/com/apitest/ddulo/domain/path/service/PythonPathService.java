package com.apitest.ddulo.domain.path.service;

import com.apitest.ddulo.domain.path.dto.request.PythonPathRequest;
import com.apitest.ddulo.domain.station.domain.Exit;
import com.apitest.ddulo.domain.station.domain.Station;
import com.apitest.ddulo.domain.station.dto.response.FastestPathResponse;
import com.apitest.ddulo.domain.station.repository.ExitRepository;
import com.apitest.ddulo.domain.station.repository.StationRepository;
import com.apitest.ddulo.domain.station.repository.TransferRepository;
import com.apitest.ddulo.domain.station.client.PythonApiClient;
import com.apitest.ddulo.domain.timetable.domain.TimeTable;
import com.apitest.ddulo.domain.timetable.repository.TimeTableRepository;
import com.apitest.ddulo.global.exception.CustomException;
import com.apitest.ddulo.global.exception.ErrorCode;
import com.apitest.ddulo.global.utils.DateUtils;
import com.apitest.ddulo.global.utils.StationUtils;
import com.apitest.ddulo.global.utils.SubwayUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PythonPathService {

    private final StationRepository stationRepository;
    private final ExitRepository exitRepository;
    private final TransferRepository transferRepository;
    private final TimeTableRepository timeTableRepository;
    private final PythonApiClient pythonApiClient;

    @Transactional(readOnly = true)
    public void triggerPythonPathCalculation(FastestPathResponse response) {
        try {
            List<FastestPathResponse.RouteLeg> legs = response.getLegs();
            if (legs.isEmpty()) return;

            // 1. 출발 정보
            FastestPathResponse.RouteLeg firstLeg = legs.get(0);
            // 1-1. 출발 역 정보
            Station startStation = stationRepository.findStationByStationNameAndLineName(
                    StationUtils.addStationSuffix(firstLeg.getStartStation()),
                    firstLeg.getLineName())
                    .orElseThrow(() -> new CustomException(ErrorCode.DATA_NOT_FOUND));
            // 1-2. 출발 방향(0 or 1)
            int startDirection = SubwayUtils.getDirectionNumberInt(firstLeg.getDirection());
            
            // 1-3. 출발역 기준 도착역의 정보 (출발 시 탑승 위치 계산, 첫 번째 구간의 목적지에 따라 결정)
            Station firstLegEndStation = stationRepository.findStationByStationNameAndLineName(
                    StationUtils.addStationSuffix(firstLeg.getEndStation()),
                    firstLeg.getLineName())
                    .orElseThrow(() -> new CustomException(ErrorCode.DATA_NOT_FOUND));

            Station secondLegStartStation = null;
            // 출발-도착 리스트가 2개 이상이면 환승이 있다는 뜻이므로, 인덱스[1]의 시작역 정보도 불러온다.
            if (legs.size() > 1) {
                secondLegStartStation = stationRepository.findStationByStationNameAndLineName(
                        StationUtils.addStationSuffix(legs.get(1).getStartStation()),
                        legs.get(1).getLineName())
                        .orElseThrow(() -> new CustomException(ErrorCode.DATA_NOT_FOUND));
            }
            // 두번째 역의 출입구
            List<Integer> startFastBoarding = getBoardingInfo(firstLegEndStation, secondLegStartStation, startDirection);
            
            // 날짜를 요일로 변환(ex: WED)
            String dayKey = DateUtils.convertDateToDayKey(String.valueOf(LocalDate.now()));

            // 시간표 조회용 방향 문자열 ("1" or "2")
            String startDirectionStr = SubwayUtils.getDirectionNumber(firstLeg.getDirection());
            // 가장 가까운 3개 열차 시간을 반올림한 값을 저장
            List<String> startTimes = getTrainTimes(startStation.getStationCode(), startDirectionStr, 0);


            // 2. 환승 정보
            List<String> transferStationIds = new ArrayList<>();    // 환승역 id
            List<Integer> transferDirections = new ArrayList<>();   // 환승 방향
            List<List<String>> transferTimes = new ArrayList<>();   // 환승 시간
            List<List<Integer>> transferFastBoardings = new ArrayList<>();  // 빠른 게이트

            // 첫번째 구간 소요시간
            int accumulatedTime = firstLeg.getSectionTime();

            // 환승 구간 반복 (ex: 역삼 - 강남 - 신사 - ...)
            for (int i = 0; i < legs.size() - 1; i++) {
                // 현재 구간의 출발역이 출발역인 Leg (역삼 - 강남)
                FastestPathResponse.RouteLeg currentLeg = legs.get(i);
                // 현재 구간의 도착역이 출발역인 Leg (강남 - 신사)
                FastestPathResponse.RouteLeg nextLeg = legs.get(i + 1);

                // 현재 구간의 도착역 정보 (강남역 2호선을 가져옴)
                Station transferStation = stationRepository.findStationByStationNameAndLineName(
                        StationUtils.addStationSuffix(currentLeg.getEndStation()),
                        currentLeg.getLineName())
                        .orElseThrow(() -> new CustomException(ErrorCode.DATA_NOT_FOUND));

                // 환승 방향 (갈아탈 노선의 운행방향 : 신사행 이므로 상행이 나옴)
                int transferDirection = SubwayUtils.getDirectionNumberInt(nextLeg.getDirection());
                // 환승 방향 문자열 ("1" or "2", 시간표 조회용)
                String transferDirectionStr = SubwayUtils.getDirectionNumber(nextLeg.getDirection());
                // 파이썬으로 보낼 환승역 id 리스트
                transferStationIds.add(transferStation.getStationCode());
                // 파이썬으로 보낼 진행방향 리스트
                transferDirections.add(transferDirection);
                // 환승역 도착 예상 시점의 열차 시간 리스트
                transferTimes.add(getTrainTimes(transferStation.getStationCode(), transferDirectionStr, accumulatedTime));

                // 다음 구간의 도착역 정보 (신사역 신분당선을 가져옴)
                Station nextLegEndStation = stationRepository.findStationByStationNameAndLineName(
                        StationUtils.addStationSuffix(nextLeg.getEndStation()),
                        nextLeg.getLineName())
                        .orElseThrow(() -> new CustomException(ErrorCode.DATA_NOT_FOUND));

                Station nextNextLegStartStation = null;
                // 다다음 구간이 또 존재한다면 로직 실행
                if (i + 2 < legs.size()) {
                    // 다다음 구간의 시작역 정보 조회(논현이 있다면 논현역)
                    FastestPathResponse.RouteLeg nextNextLeg = legs.get(i + 2);
                    nextNextLegStartStation = stationRepository.findStationByStationNameAndLineName(
                            StationUtils.addStationSuffix(nextNextLeg.getStartStation()),
                            nextNextLeg.getLineName())
                            .orElseThrow(() -> new CustomException(ErrorCode.DATA_NOT_FOUND));
                }
                /*
                * 가장 좋은 칸 번호가 나옴
                * 산출 로직 :
                * 지금 갈아타는 열차(강남, 신분당선)를 탈 때,
                * 다음 역(신사)에서의 환승or하차를 고려해서 가장 좋은 칸 번호를 도출
                * */
                transferFastBoardings.add(getBoardingInfo(nextLegEndStation, nextNextLegStartStation, transferDirection));
                
                // 다음 루프를 위해 소요시간을 누적
                accumulatedTime += nextLeg.getSectionTime();
            }

            // 3. 도착 정보
            FastestPathResponse.RouteLeg lastLeg = legs.get(legs.size() - 1);
            // 마지막 구간(leg)의 도착역 정보를 조회
            Station endStation = stationRepository.findStationByStationNameAndLineName(
                    StationUtils.addStationSuffix(lastLeg.getEndStation()),
                    lastLeg.getLineName())
                    .orElseThrow(() -> new CustomException(ErrorCode.DATA_NOT_FOUND));

            // 파이썬 요청 객체 Build
            PythonPathRequest request = PythonPathRequest.builder()
                    .dayOfWeek(dayKey)
                    .startStationId(startStation.getStationCode())
                    .startDirection(startDirection)
                    .startTime(startTimes)
                    .startFastBoarding(startFastBoarding)
                    .transferStationIds(transferStationIds)
                    .transferDirections(transferDirections)
                    .transferTimes(transferTimes)
                    .transferFastBoardings(transferFastBoardings)
                    .endStationId(endStation.getStationCode())
                    .build();

            pythonApiClient.requestPathCalculation(request);

        } catch (Exception e) {
            log.error("Failed to trigger python path calculation", e);
        }
    }

    private List<Integer> getBoardingInfo(Station currentEndStation, Station nextStartStation, int direction) {
        // 1. 환승인 경우 (다음 노선 역 정보가 있음)
        if (nextStartStation != null) {
            return transferRepository.findByFromStationAndToStation(currentEndStation, nextStartStation)
                    .map(t -> List.of(t.getCarNumber(), t.getDoorNumber()))
                    .orElse(List.of(1, 1)); // 정보 없으면 기본값
        }
        
        // 2. 도착인 경우 (다음 노선 역 정보 없음)
        else {
            List<Exit> exits = exitRepository.findByStationAndDirection(currentEndStation, direction);
            if (exits.isEmpty()) return List.of(1, 1);
            Exit exit = exits.get(0);
            return List.of(exit.getCarNumber(), exit.getDoorNumber());
        }
    }

    // DB의 시간표를 조회해서 현재 시간, 역, 방향 기준 가장 가까운 3개 열차 시간을 리턴
    private List<String> getTrainTimes(String stationCode, String direction, int addedSeconds) {
        String dayKey = DateUtils.convertDateToDayKey(String.valueOf(LocalDate.now()));
        String weekTag = DateUtils.convertDayKeyToWeekTag(dayKey);
        
        LocalTime targetTime = LocalTime.now().plusSeconds(addedSeconds);
        String targetTimeStr = targetTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        return timeTableRepository.findNextTrains(
                stationCode, weekTag, direction, targetTimeStr, PageRequest.of(0, 3))
                .stream()
                .map(TimeTable::getLeftTime)
                .map(DateUtils::roundToNearest10Minutes)
                .toList();
    }
}
