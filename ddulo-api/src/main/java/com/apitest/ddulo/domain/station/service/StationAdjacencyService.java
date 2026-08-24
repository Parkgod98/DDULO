package com.apitest.ddulo.domain.station.service;

import com.apitest.ddulo.domain.station.domain.Station;
import com.apitest.ddulo.domain.station.dto.internal.StationAdjacencyResult;
import com.apitest.ddulo.domain.station.dto.internal.StationNode;
import com.apitest.ddulo.domain.station.repository.StationRepository;
import com.apitest.ddulo.global.exception.CustomException;
import com.apitest.ddulo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StationAdjacencyService {

    private final StationRepository stationRepository;

    // 인접 리스트: Key=역코드, Value=연결된 역코드(이전/이후)
    private final Map<String, AdjacencyInfo> networkMap = new HashMap<>();

    private static class AdjacencyInfo {
        Set<String> prev = new LinkedHashSet<>(); // 이전 역들
        Set<String> next = new LinkedHashSet<>(); // 다음 역들
    }

    @PostConstruct
    public void init() {
        // 노선 데이터 정의
        initializeLine1();
        initializeLine2();
        initializeLine3();
        initializeLine4();
        initializeLine5();
        initializeLine6();
        initializeLine7();
        initializeLine8();
        initializeLine9();
        initializeArex();
        initializeShinbundang();
        initializeSuinBundang();
    }

    /**
     * 메인 메서드: 특정 역코드를 넣으면 인접한 역(이전/이후) 리스트를 반환
     */
    public StationAdjacencyResult getAdjacentStations(String stationCode) {
        if (!networkMap.containsKey(stationCode)) {
            return StationAdjacencyResult.builder()
                    .prevStations(Collections.emptyList())
                    .nextStations(Collections.emptyList())
                    .build();
        }

        AdjacencyInfo info = networkMap.get(stationCode);

        // 이전 역 리스트 변환
        List<StationNode> prevNodes = info.prev.stream()
                .map(this::createNode)
                .collect(Collectors.toList());

        // 다음 역 리스트 변환
        List<StationNode> nextNodes = info.next.stream()
                .map(this::createNode)
                .collect(Collectors.toList());

        return StationAdjacencyResult.builder()
                .prevStations(prevNodes)
                .nextStations(nextNodes)
                .build();
    }

    private StationNode createNode(String code) {
        return StationNode.builder()
                .stationCode(code)
                .stationName(getStationName(code))
                .build();
    }

    // 그래프 연결 헬퍼 메서드

    // 양방향 연결 (대부분의 노선)
    private void connectBidirectional(String... codes) {
        for (int i = 0; i < codes.length - 1; i++) {
            String current = codes[i];      // 현재 역 (예: 141 구로)
            String nextTarget = codes[i+1]; // 다음 역 (예: 142 구일)

            // Current 입장에서 NextTarget은 '다음 역'
            getAdjacencyInfo(current).next.add(nextTarget);

            // NextTarget 입장에서 Current는 '이전 역'
            getAdjacencyInfo(nextTarget).prev.add(current);
        }
    }

    // 단방향 연결 (6호선 순환 등)
    private void connectOneWay(String from, String to) {
        // From 입장에서 To는 '다음 역'
        getAdjacencyInfo(from).next.add(to);

        // To 입장에서 From은 '이전 역'
        getAdjacencyInfo(to).prev.add(from);
    }

    // 맵에서 Info 가져오기 (없으면 생성)
    private AdjacencyInfo getAdjacencyInfo(String code) {
        return networkMap.computeIfAbsent(code, k -> new AdjacencyInfo());
    }

    // DB에서 역 이름 조회
    private String getStationName(String stationCode) {
        return stationRepository.findByStationCode(stationCode)
                .map(Station::getStationName) // Station 엔티티에 getStationName()이 있다고 가정
                .orElse("알수없음"); // DB에 없을 경우 기본값
    }


    // 노선별 초기화 로직

    private void initializeLine1() {
        // 본선: 소요산(100)~구로(141)~인천(161)
        List<String> mainLine = new ArrayList<>();
        for (int i = 100; i <= 161; i++) mainLine.add(String.valueOf(i));
        connectBidirectional(mainLine.toArray(new String[0]));

        // 경부선: 구로(141)~가산(P142)~신창(P177)
        // 141과 P142 연결
        List<String> kyungbuLine = new ArrayList<>();
        kyungbuLine.add("141");
        for (int i = 142; i <= 177; i++) kyungbuLine.add("P" + i);
        connectBidirectional(kyungbuLine.toArray(new String[0]));

        // 경원선 지선: 소요산(100)~청산(100-1)~연천(100-3)
        // 100-1, 100-2, 100-3
        connectBidirectional("100", "100-1", "100-2", "100-3");

        // 광명 셔틀: 금천구청(P144)~광명(P144-1)
        connectBidirectional("P144", "P144-1");

        // 서동탄 지선: 병점(P157)~서동탄(P157-1)
        connectBidirectional("P157", "P157-1");
    }

    private void initializeLine2() {
        // 본선 순환: 201~243 연결, 그리고 243과 201 연결
        List<String> loopLine = new ArrayList<>();
        for (int i = 201; i <= 243; i++) loopLine.add(String.valueOf(i));
        connectBidirectional(loopLine.toArray(new String[0]));
        connectBidirectional("243", "201"); // 순환 고리 완성

        // 성수지선: 성수(211) ~ 신설동(211-4)
        // 211, 211-1, 211-2, 211-3, 211-4
        connectBidirectional("211", "211-1", "211-2", "211-3", "211-4");

        // 신도림지선: 신도림(234) ~ 까치산(234-4)
        connectBidirectional("234", "234-1", "234-2", "234-3", "234-4");
    }

    private void initializeLine3() {
        // 대화(309)~오금(352)
        List<String> line = new ArrayList<>();
        for (int i = 309; i <= 352; i++) line.add(String.valueOf(i));
        connectBidirectional(line.toArray(new String[0]));
    }

    private void initializeLine4() {
        // 진접(405)~오이도(456) *407 결번 처리
        List<String> line = new ArrayList<>();
        for (int i = 405; i <= 456; i++) {
            if (i == 407) continue; // 407번 제외
            line.add(String.valueOf(i));
        }
        connectBidirectional(line.toArray(new String[0]));
    }

    private void initializeLine5() {
        // 본선: 방화(510)~강동(548)
        List<String> main = new ArrayList<>();
        for (int i = 510; i <= 548; i++) main.add(String.valueOf(i));
        connectBidirectional(main.toArray(new String[0]));

        // 하남 지선: 강동(548)~하남검단산(558)
        List<String> hanam = new ArrayList<>();
        hanam.add("548"); // 분기점 포함
        for (int i = 549; i <= 558; i++) hanam.add(String.valueOf(i));
        connectBidirectional(hanam.toArray(new String[0]));

        // 마천 지선: 강동(548)~둔촌동(P549)~마천(P555)
        List<String> macheon = new ArrayList<>();
        macheon.add("548");
        for (int i = 549; i <= 555; i++) macheon.add("P" + i);
        connectBidirectional(macheon.toArray(new String[0]));
    }

    private void initializeLine6() {
        // 응암 순환(단방향): 응암(610)->역촌(611)->...->구산(615)->응암(610)
        // 역코드가 610~615 사이를 순환한다고 가정
        connectOneWay("610", "611");
        connectOneWay("611", "612");
        connectOneWay("612", "613");
        connectOneWay("613", "614");
        connectOneWay("614", "615");
        connectOneWay("615", "610"); // 다시 응암으로

        // 본선(양방향): 응암(610)~신내(648)
        // 610에서 616으로 가는 길은 양방향 (순환에서 빠져나오거나 들어가는 길)
        List<String> main = new ArrayList<>();
        main.add("610"); // 시점
        for (int i = 616; i <= 648; i++) main.add(String.valueOf(i));
        connectBidirectional(main.toArray(new String[0]));
    }

    private void initializeLine7() {
        // 장암(709)~석남(761)
        List<String> line = new ArrayList<>();
        for (int i = 709; i <= 761; i++) line.add(String.valueOf(i));
        connectBidirectional(line.toArray(new String[0]));
    }

    private void initializeLine8() {
        // 별내(804)~모란(827)
        List<String> line = new ArrayList<>();
        for (int i = 804; i <= 827; i++) line.add(String.valueOf(i));
        connectBidirectional(line.toArray(new String[0]));
    }

    private void initializeLine9() {
        // 개화(901)~중앙보훈병원(938)
        List<String> line = new ArrayList<>();
        for (int i = 901; i <= 938; i++) line.add(String.valueOf(i));
        connectBidirectional(line.toArray(new String[0]));
    }

    private void initializeArex() {
        // 공항철도: A01 ~ A11 (중간 코드 불규칙적이므로 직접 배열 선언)
        // 서울역(A01)~디지털미디어시티(A04)~마곡나루(A042)~김포공항(A05)~검암(A07)~
        // 청라국제도시(A071)~영종(A072)~운서(A08)~인천공항2터미널(A11)
        String[] stations = {
                "A01", "A02", "A03", "A04", "A042", "A05",
                "A06", "A07", "A071", "A072", "A08", "A09", "A10", "A11"
        };
        connectBidirectional(stations);
    }

    private void initializeShinbundang() {
        // 신사(D04)~광교(D19)
        List<String> line = new ArrayList<>();
        for (int i = 4; i <= 19; i++) line.add(String.format("D%02d", i)); // D04, D05...
        connectBidirectional(line.toArray(new String[0]));
    }

    private void initializeSuinBundang() {
        // 청량리(K209)~인천(K272)
        List<String> line = new ArrayList<>();
        for (int i = 209; i <= 272; i++) line.add("K" + i);
        connectBidirectional(line.toArray(new String[0]));
    }

    //역코드 유효성 검사
    public void validateStationExists(String stationCode) {
        if (!stationRepository.existsByStationCode(stationCode)) {
            throw new CustomException(ErrorCode.STATION_NOT_FOUND);
        }
    }
}
