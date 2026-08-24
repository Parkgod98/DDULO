package com.apitest.ddulo.domain.path.service;

import com.apitest.ddulo.domain.path.dto.external.redis.PathPredictionData;
import com.apitest.ddulo.domain.path.dto.response.PathPredictionResponse;
import com.apitest.ddulo.domain.station.dto.response.FastestPathResponse;
import com.apitest.ddulo.domain.station.repository.StationRepository;
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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class PathPredictionService {

    private final PathRedisService pathRedisService;
    private final StationRepository stationRepository;
    private final TimeTableRepository timeTableRepository;

    @Transactional(readOnly = true)
    public PathPredictionResponse predictPathDetails(FastestPathResponse fastestPathResponse) {
        var leg = fastestPathResponse.getLegs();

        //경로의 시작역 이름과 노선명 추출
        String startStationName = leg.get(0).getStartStation();
        String startLineName = leg.get(0).getLineName();
        //경로의 도착역 이름과 노선명 추출
        String endStationName = leg.get(fastestPathResponse.getLegs().size() - 1).getEndStation();
        String endLineName = leg.get(fastestPathResponse.getLegs().size() - 1).getLineName();
    
        //출발역 코드 조회
        String startCode = stationRepository.findStationCodeByNameAndLine(
                        StationUtils.addStationSuffix(startStationName), startLineName)
                .orElseThrow(() -> new CustomException(ErrorCode.DATA_NOT_FOUND));
        //도착역 코드 조회
        String endCode = stationRepository.findStationCodeByNameAndLine(
                        StationUtils.addStationSuffix(endStationName), endLineName)
                .orElseThrow(() -> new CustomException(ErrorCode.DATA_NOT_FOUND));
        
        //Redis에서 경로 예측 요약 정보 조회
        PathPredictionData rawData = pathRedisService.getPathPrediction(startCode, endCode);

        //데이터 없는 경우 방어로직
        if(rawData == null)
            return buildDefaultPrediction(fastestPathResponse.getTotalTime());

        return mapToDTO(rawData, fastestPathResponse, startCode);
    }

    // PathPredictionResponse로 변환하는 로직
    private PathPredictionResponse mapToDTO(
            PathPredictionData data,
            FastestPathResponse fastestPathResponse,
            String stationCode) {
        //재료준비
        LocalDate nowDate = LocalDate.now();    //2026-02-02
        LocalTime nowTime = LocalTime.now();    //02:08:21
        String currentTimeStr = nowTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        //요일을 변환 -> "1"(평일)
        String weekTag = DateUtils.convertDayKeyToWeekTag(DateUtils.convertDateToDayKey(String.valueOf(nowDate)));
        //방향을 변환 -> "1"(내선)
        String direction = SubwayUtils.getDirectionNumber(fastestPathResponse.getLegs().get(0).getDirection());

        //가장 가까운 시간의 열차 3개 조회
        List<TimeTable> nextTrains = timeTableRepository.findNextTrains(
                stationCode, weekTag, direction, currentTimeStr, PageRequest.of(0, 3)
        );
        //막차 끊기면 null 반환
        if (nextTrains.isEmpty()) return null;

        // 예측정보 리스트업
        var scheduleList = data.getResults().get(0).getSchedule();
        List<PathPredictionResponse.PredictionDetail> details = IntStream.range(0, nextTrains.size())
                .mapToObj(i -> {
                    TimeTable train = nextTrains.get(i); // i번째 열차 시간표
                    // Redis 데이터가 DB 데이터보다 적을 수 있으므로 안전장치 (IndexOutOfBounds 방지)
                    int probability = 0;
                    if (i < scheduleList.size()) {
                        probability = scheduleList.get(i).getBoardingProbability();
                    }
                    // 탑승 시간 파싱
                    LocalTime trainTime = LocalTime.parse(train.getShowTime());
                    LocalDateTime boardingTime = LocalDateTime.of(nowDate, trainTime);

                    // 대기 시간 계산
                    long waitSeconds = Math.max(0, Duration.between(nowTime, trainTime).getSeconds());

                    // 도착 시간 계산 (탑승시간 + 총 소요시간)
                    LocalDateTime arrivalTime = boardingTime.plusSeconds(fastestPathResponse.getTotalTime());

                    // 상세 객체 빌드
                    return PathPredictionResponse.PredictionDetail.builder()
                            .estimatedBoardingTime(boardingTime)
                            .estimatedArrivalTime(arrivalTime)
                            .waitingTimeSeconds(waitSeconds)
                            .boardingProbability(probability)
                            .build();
                })
                .toList();

        // 최종 Response 빌드
        return PathPredictionResponse.builder()
                .predictions(details)
                .build();
    }

    // 디폴트용
    private PathPredictionResponse buildDefaultPrediction(int totalTime) {
        LocalDateTime now = LocalDateTime.now();

        // Detail 객체를 먼저 하나 만들기 (기본값)
        PathPredictionResponse.PredictionDetail defaultDetail = PathPredictionResponse.PredictionDetail.builder()
                .estimatedBoardingTime(now)
                .estimatedArrivalTime(now.plusSeconds(totalTime))
                .waitingTimeSeconds(0)
                .boardingProbability(0)
                .build();

        // 리스트에 담아서 Response를 빌드
        return PathPredictionResponse.builder()
                .predictions(Collections.singletonList(defaultDetail))
                .build();
    }
}
