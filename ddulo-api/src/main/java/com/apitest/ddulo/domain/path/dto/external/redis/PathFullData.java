package com.apitest.ddulo.domain.path.dto.external.redis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PathFullData {

    private PathContext context;    //응답이 잘 됐는지 확인하는 용도
    private long totalTimeSecond;   //총 소요시간
    private String estimatedBoardingTime; //2026-01-28T14:30:00
    private double boardingProbability;   //탑승가능 확률

    private List<StationInfo> startStation; //시작역
    private List<TransferSection> transferStation;  //환승역
    private StationInfo endStation;     //도착역

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PathContext {
        private String targetDate;  //조회일(오늘날짜)
        private String dayOfWeek;   //요일(ex: WED)
        private String timeRange;   //시간대(ex: 1800)
        private String redisKey;    //레디스 키(ex: path:pred:221:748:WED:1800)
    }

    // 환승 구간을 감싸는 래퍼 클래스
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransferSection {
        private List<StationInfo> stations; //환승이 여러개일 수 있으니 List로 한번 더 감싸기
    }

    // 역 정보를 담는 공통 클래스 (Start, Transfer, End 모두 사용)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StationInfo {
        private String stationId;      //역 ID(역코드)
        private String stationName; //역 이름
        private String lineName;    //노선명
        private int direction;      //운행방향(0:상행/외선, 1:하행/내선)
        private int estimatedWaitingSec;    //(열차 도착까지)예상 대기시간

        // JSON 키는 "isBoardable"이지만, 자바 필드명은 boardable로 하고 매핑을 명시
        // (Lombok getter 생성 시 혼동 방지)
        @JsonProperty("isBoardable")
        private boolean boardable;   //탑승가능 여부

        private List<DetailResult> results;     //연산 결과 List
    }

    // 상세 혼잡도 및 도착 정보
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailResult {
        private List<CarCongestion> carCongestions;     //열차 혼잡도
        private List<DoorCongestion> stationCongestions; //역 혼잡도
        private List<DoorCongestion> totalCongestions;  //역 + 열차 종합 혼잡도
        private List<BoardingSpot> bestBoardings;    //베스트탑승 위치
        private List<BoardingSpot> comfortBoarding; //쾌적한탑승 위치
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CarCongestion {
        private int carNo;  //객차번호
        private int congestionLevel;    //혼잡도
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoorCongestion {
        private int carNo;  //객차번호
        private int doorNo; //문번호
        private int congestionLevel;    //혼잡도
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BoardingSpot {
        private int carNo;  //객차번호
        private int doorNo; //문번호
    }
}
