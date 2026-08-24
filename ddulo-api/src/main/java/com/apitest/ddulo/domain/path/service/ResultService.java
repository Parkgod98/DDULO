package com.apitest.ddulo.domain.path.service;

import com.apitest.ddulo.domain.path.dto.external.redis.PathFullData;
import com.apitest.ddulo.domain.path.dto.response.PathPredictionResponse;
import com.apitest.ddulo.domain.path.dto.response.ResultResponse;
import com.apitest.ddulo.domain.station.dto.response.FastestPathResponse;
import com.apitest.ddulo.domain.station.repository.StationRepository;
import com.apitest.ddulo.global.exception.CustomException;
import com.apitest.ddulo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResultService {

    private final PathRedisService pathRedisService;
    private final StationRepository stationRepository;

    /**
     * 최단 경로 정보와 예측 정보를 바탕으로 최종 결과 응답을 생성합니다.
     * 
     * @param fastestPathResponse 최단 경로 정보 (SK API 등)
     * @param pathPredictionResponse 경로 예측 정보 (Redis 요약 정보)
     * @return 최종 결과 응답 (ResultResponse)
     */
    @Transactional(readOnly = true)
    public ResultResponse getResult(
            FastestPathResponse fastestPathResponse,
            PathPredictionResponse pathPredictionResponse){
        return buildResultResponse(fastestPathResponse, pathPredictionResponse);
    }
    
    // 최적경로정보로 최종결과반환DTO를 만드는 메서드
    private ResultResponse buildResultResponse(
            FastestPathResponse fastestPathResponse,
            PathPredictionResponse pathPredictionResponse) {
        
        // 1. 최단 경로의 구간(Leg) 정보 가져오기
        List<FastestPathResponse.RouteLeg> legs = fastestPathResponse.getLegs();

        // 2. 출발역과 도착역의 역 코드 조회 (Redis Key 생성을 위해 필요)
        // 출발역: 첫 번째 구간의 시작역
        String startStationCode = getStationCode(
                legs.get(0).getStartStation(),
                legs.get(0).getLineName()
        );
        // 도착역: 마지막 구간의 도착역
        String endStationCode = getStationCode(
                legs.get(legs.size() - 1).getEndStation(),
                legs.get(legs.size() - 1).getLineName()
        );

        // 3. Redis에서 상세 경로 정보(PathFullData) 조회
        // Python 서버가 계산하여 적재한 데이터를 가져옴
        PathFullData pathFullData = pathRedisService.getFullPath(startStationCode, endStationCode);
        
        // 데이터가 없으면 빈 객체 반환 (Python 서버 적재 지연 또는 오류 가능성)
        if (pathFullData == null) {
            return ResultResponse.builder().build();
        }

        // 4. 조회된 데이터를 ResultResponse DTO로 매핑하여 반환
        return ResultResponse.builder()
                .totalTimeSecond((int) pathFullData.getTotalTimeSecond()) // 총 소요 시간
                .estimatedBoardingTime(LocalDateTime.parse(pathFullData.getEstimatedBoardingTime())) // 예상 탑승 시간
                .boardingProbability(pathFullData.getBoardingProbability()) // 탑승 확률
                
                // 출발역 상세 정보 매핑
                .startStation(mapStationInfoList(pathFullData.getStartStation()))
                
                // 환승역 상세 정보 매핑 (리스트 형태)
                .transferStation(mapTransferStations(pathFullData.getTransferStation()))

                // 도착역 상세 정보 매핑
                .endStation(mapStationInfo(pathFullData.getEndStation()))
                .build();
    }

    // 역 이름과 노선명으로 역 코드를 조회하는 헬퍼 메서드
    private String getStationCode(String stationName, String lineName) {
        return stationRepository.findStationCodeByNameAndLine(stationName, lineName)
                .orElseThrow(() -> new CustomException(ErrorCode.STATION_NOT_FOUND));
    }

    // List<PathFullData.StationInfo> -> List<ResultResponse.StationInfo> 변환
    private List<ResultResponse.StationInfo> mapStationInfoList(List<PathFullData.StationInfo> sourceList) {
        if (sourceList == null) return null;
        return sourceList.stream()
                .map(this::mapStationInfo)
                .toList();
    }

    // PathFullData.StationInfo -> ResultResponse.StationInfo 변환 (단건)
    private ResultResponse.StationInfo mapStationInfo(PathFullData.StationInfo source) {
        if (source == null) return null;
        return ResultResponse.StationInfo.builder()
                .stationCode(source.getStationId())
                .stationName(source.getStationName())
                .lineName(source.getLineName())
                .estimatedWaitingSec(source.getEstimatedWaitingSec()) // 예상 대기 시간
                .isBoardable(source.isBoardable()) // 탑승 가능 여부
                .results(mapResultInfoList(source.getResults())) // 상세 결과 리스트 매핑
                .build();
    }

    // 환승역 정보 리스트 매핑 (인덱스를 활용하여 환승 순서 부여)
    private List<ResultResponse.TransferStationGroup> mapTransferStations(List<PathFullData.TransferSection> sourceList) {
        if (sourceList == null) return null;
        return IntStream.range(0, sourceList.size())
                .mapToObj(i -> ResultResponse.TransferStationGroup.builder()
                        .transferOrder(i + 1) // 환승 순서 (1부터 시작)
                        .options(mapStationInfoList(sourceList.get(i).getStations())) // 해당 환승역의 옵션들
                        .build())
                .toList();
    }

    // 상세 결과 리스트 매핑
    private List<ResultResponse.ResultInfo> mapResultInfoList(List<PathFullData.DetailResult> sourceList) {
        if (sourceList == null) return null;
        return sourceList.stream()
                .map(this::mapResultInfo)
                .toList();
    }

    // 상세 결과 매핑 (혼잡도, 추천 위치 등)
    private ResultResponse.ResultInfo mapResultInfo(PathFullData.DetailResult source) {
        if (source == null) return null;
        return ResultResponse.ResultInfo.builder()
                .carCongestions(mapCarCongestions(source.getCarCongestions())) // 칸별 혼잡도
                .stationCongestions(mapStationCongestions(source.getStationCongestions())) // 역 혼잡도
                .totalCongestions(mapStationCongestions(source.getTotalCongestions())) // 종합 혼잡도
                .bestBoardings(mapBoardingInfos(source.getBestBoardings())) // 추천 탑승 위치 (Best)
                .comfortBoarding(mapBoardingInfos(source.getComfortBoarding())) // 추천 탑승 위치 (Comfort)
                .build();
    }

    // 칸별 혼잡도 리스트 매핑
    private List<ResultResponse.CarCongestion> mapCarCongestions(List<PathFullData.CarCongestion> sourceList) {
        if (sourceList == null) return null;
        return sourceList.stream()
                .map(s -> ResultResponse.CarCongestion.builder()
                        .carNo(s.getCarNo())
                        .congestionLevel(s.getCongestionLevel())
                        .build())
                .toList();
    }

    // 역/종합 혼잡도 리스트 매핑
    private List<ResultResponse.StationCongestion> mapStationCongestions(List<PathFullData.DoorCongestion> sourceList) {
        if (sourceList == null) return null;
        return sourceList.stream()
                .map(s -> ResultResponse.StationCongestion.builder()
                        .carNo(s.getCarNo())
                        .doorNo(s.getDoorNo())
                        .congestionLevel(s.getCongestionLevel())
                        .build())
                .toList();
    }

    // 탑승 위치 정보 리스트 매핑
    private List<ResultResponse.BoardingInfo> mapBoardingInfos(List<PathFullData.BoardingSpot> sourceList) {
        if (sourceList == null) return null;
        return sourceList.stream()
                .map(s -> ResultResponse.BoardingInfo.builder()
                        .carNo(s.getCarNo())
                        .doorNo(s.getDoorNo())
                        .build())
                .toList();
    }
}
