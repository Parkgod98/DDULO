package com.apitest.ddulo.domain.station.service;

import com.apitest.ddulo.domain.metadata.domain.ApiMetadata;
import com.apitest.ddulo.domain.metadata.repository.ApiMetadataRepository;
import com.apitest.ddulo.domain.metadata.service.ApiMetadataService;
import com.apitest.ddulo.domain.station.client.TransferApiClient;
import com.apitest.ddulo.domain.station.domain.Station;
import com.apitest.ddulo.domain.station.domain.Transfer;
import com.apitest.ddulo.domain.station.dto.external.openapi.FastTransferData;
import com.apitest.ddulo.domain.station.repository.StationRepository;
import com.apitest.ddulo.domain.station.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferSyncService {

    private static final String API_NAME = "FAST_TRANSFER";

    private final TransferRepository transferRepository;
    private final StationRepository stationRepository;
    private final TransferApiClient transferApiClient;
    private final ApiMetadataRepository apiMetadataRepository;
    private final ApiMetadataService apiMetadataService;

    // 허용된 노선과 운영기관 매핑 (검증용 지도)
    private static final Map<String, Set<String>> ALLOWED_MAP = new HashMap<>();

    static {
        // 1호선 & 수인분당선 -> 코레일
        addRule("1호선", "코레일");
        addRule("수인분당선", "코레일");

        // 2~8호선 -> 서울교통공사
        List.of("2호선", "3호선", "4호선", "5호선", "6호선", "7호선", "8호선")
                .forEach(line -> addRule(line, "서울교통공사"));

        // 9호선 -> 서울시메트로9호선주식회사
        addRule("9호선", "서울시메트로9호선주식회사");

        // 신분당선 -> 둘 다 허용
        addRule("신분당선", "서울교통공사");
        addRule("신분당선", "네오트랜스주식회사");

        // 공항철도
        addRule("공항철도", "공항철도주식회사");
    }

    private static void addRule(String line, String operator) {
        ALLOWED_MAP.computeIfAbsent(line, k -> new HashSet<>()).add(operator);
    }

    // 초기화 로직 (API 호출 -> 저장)
    @Transactional
    public void syncTransferInfoIfNeeded() {
        // 데이터 존재 여부 확인
        if (!apiMetadataService.isApiCallNeeded(API_NAME)) {
            log.info("Transfer 데이터가 이미 존재하므로, 다음으로 넘어갑니다.");
            return;
        }
        log.info("Transfer 데이터를 찾을 수 없으므로, 동기화를 실행합니다.");

        syncStation();

        //성공 후 메타데이터 갱신 (없으면 생성, 있으면 업데이트)
        ApiMetadata metadata = apiMetadataRepository.findByApiName(API_NAME)
                .orElseGet(() -> ApiMetadata.builder()
                        .apiName(API_NAME)
                        .updateIntervalDay(null) // null : 최초 1회만, N : N일마다 업데이트
                        .build());

        metadata.markUpdated(); // 현재 시간 찍기
        apiMetadataRepository.save(metadata);
    }

    @Transactional
    public void syncStation(){
        int page = 1;
        int perPage = 500;
        boolean hasNext = true;

        while (hasNext) {
            FastTransferData response = null;
            try {
                response = transferApiClient.fetchTransferData(page, perPage);
            } catch (Exception e) {
                log.error("API call failed: {}", e.getMessage());
                break;
            }

            if (response == null || response.getData() == null || response.getData().isEmpty()) {
                log.warn("No data found or API error at page {}", page);
                break;
            }

            // 데이터 저장 (트랜잭션 처리됨)
            syncTransferData(response.getData());

            // 페이지네이션 체크
            int totalCount = response.getTotalCount();
            int currentFetched = page * perPage;

            if (currentFetched >= totalCount) {
                hasNext = false;
            } else {
                page++;
            }
        }
        log.info("Transfer Data Synchronization Completed.");
    }

    @Transactional
    public void syncTransferData(List<FastTransferData.Data> dataList) {
        int count = 0;

        for (FastTransferData.Data dto : dataList) {
            // 4가지 조건 검증 (출발 조건 && 도착 조건)
            if (isValid(dto.getLineName(), dto.getOperator()) &&
                    isValid(dto.getTransferLineName(), dto.getTransferOperator())) {

                saveTransferInfo(dto);
                count++;
            }
        }
        log.info("총 {}건 중 {}건의 환승 데이터 저장 완료", dataList.size(), count);
    }

    // 검증 메서드 (핵심 로직)
    private boolean isValid(String line, String operator) {
        // 노선이 맵에 있고, 그 노선의 허용된 운영기관에 포함되는지 확인
        return ALLOWED_MAP.containsKey(line) && ALLOWED_MAP.get(line).contains(operator);
    }

    // 저장 메서드
    private void saveTransferInfo(FastTransferData.Data dto) {
        // 실제 Station 엔티티 찾기 (DB 조회)
        String cleanFromName = formatStationName(dto.getStationName());
        String cleanToName = formatStationName(dto.getTransferStationName());

        Station fromStation = stationRepository.findFirstByStationNameAndLineName(cleanFromName, dto.getLineName())
                .orElse(null);

        Station toStation = stationRepository.findFirstByStationNameAndLineName(cleanToName, dto.getTransferLineName())
                .orElse(null);

        // 역 정보를 못 찾으면 저장 불가 (로그 남기고 스킵)
        if (fromStation == null || toStation == null) {
             log.warn("역 정보를 찾을 수 없음: {} -> {}", dto.getStationName(), dto.getTransferStationName());
            return;
        }

        // 엔티티 생성 및 저장
        Transfer transfer = Transfer.builder()
                .fromStation(fromStation)
                .toStation(toStation)
                .carNumber(dto.getCarNumber())
                .doorNumber(dto.getDoorNumber())
                .build();

        transferRepository.save(transfer);
    }

    //역이름 전처리 메서드
    private String formatStationName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return "";
        }

        // ex: "공릉(서울과학기술대)" -> ["공릉", "서울과학기술대)"] -> "공릉" 선택
        String name = rawName.split("\\(")[0];

        // 이미 '역'까지 붙은 경우(ex: "서울역" -> 그대로 "서울역") 반환
        if (name.endsWith("역")) {
            return name;
        }

        return name.trim() + "역";
    }
}
