package com.apitest.ddulo.domain.station.service;

import com.apitest.ddulo.domain.metadata.domain.ApiMetadata;
import com.apitest.ddulo.domain.metadata.repository.ApiMetadataRepository;
import com.apitest.ddulo.domain.metadata.service.ApiMetadataService;
import com.apitest.ddulo.domain.station.client.SeoulApiClient;
import com.apitest.ddulo.domain.station.domain.Exit;
import com.apitest.ddulo.domain.station.domain.Station;
import com.apitest.ddulo.domain.station.dto.external.openapi.FastExitData;
import com.apitest.ddulo.domain.station.repository.ExitRepository;
import com.apitest.ddulo.domain.station.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExitDataSyncService {

    private static final String API_NAME = "SEOUL_EXIT_INFO";

    private final SeoulApiClient seoulApiClient;
    private final ExitRepository exitRepository;
    private final StationRepository stationRepository;
    private final ApiMetadataRepository apiMetadataRepository;
    private final ApiMetadataService apiMetadataService;

    //데이터 존재하는지 확인하고 동기화 여부 선택
    @Transactional
    public void syncExitInfoIfNeeded() {
        // 가져올 필요 있는지 판단
        if (!apiMetadataService.isApiCallNeeded(API_NAME)) {
            log.info("Exit 데이터가 이미 존재하므로, 다음으로 넘어갑니다.");
            return;
        }
        log.info("Exit 데이터를 찾을 수 없으므로, 동기화를 실행합니다.");

        // 외부 API 호출
        syncExitInfo();

        //성공 후 메타데이터 갱신 (없으면 생성, 있으면 업데이트)
        ApiMetadata metadata = apiMetadataRepository.findByApiName(API_NAME)
                .orElseGet(() -> ApiMetadata.builder()
                        .apiName(API_NAME)
                        .updateIntervalDay(null) // null : 최초 1회만, N : N일마다 업데이트
                        .build());

        metadata.markUpdated(); // 현재 시간 찍기
        apiMetadataRepository.save(metadata);
    }

    //1부터 시작해서 500개씩 불러오기. 전체데이터수보다 endIndex가 크면 종료.
    @Transactional
    public void syncExitInfo() {
        int startIndex = 1;
        int limit = 500;
        int endIndex = 500;
        boolean hasNext = true;

        while (hasNext) {
            log.info("Fetching exit info from {} to {}", startIndex, endIndex);
            FastExitData response = null;
            try {
                response = seoulApiClient.fetchExitInfo(startIndex, endIndex);
            } catch (Exception e) {
                log.error("API call failed: {}", e.getMessage());
                break;
            }

            if (response == null || response.getResponse() == null || response.getResponse().getBody() == null) {
                log.error("Failed to fetch exit info or invalid response structure");
                break;
            }

            FastExitData.Body body = response.getResponse().getBody();
            if (body.getItems() == null || body.getItems().getItem() == null) {
                log.warn("No items found in response body.");
                break;
            }

            List<FastExitData.Item> items = body.getItems().getItem();
            for (FastExitData.Item item : items) {
                saveExitInfo(item);
            }

            int totalCount = body.getTotalCount();
            if (endIndex >= totalCount) {
                hasNext = false;
            } else {
                startIndex += limit;
                endIndex += limit;
            }
        }
        log.info("Exit info sync completed.");
    }

    //데이터 파싱 후 저장
    private void saveExitInfo(FastExitData.Item item) {
        String stationCode = item.getStationCode();

        // Station 조회
        Optional<Station> stationOpt = stationRepository.findByStationCode(stationCode);

        // 없으면 앞의 '0'을 제거하고 검색 (예: "0150" -> "150")
        if (stationOpt.isEmpty() && stationCode != null && stationCode.startsWith("0")) {
            String strippedCode = stationCode.replaceFirst("^0+(?!$)", "");
            stationOpt = stationRepository.findByStationCode(strippedCode);
        }

        if (stationOpt.isEmpty()) {
            log.warn("Station not found for code: {}", stationCode);
            return;
        }

        // Direction 변환 ("상행" -> 0, "하행" -> 1)
        int direction = parseDirection(item.getDirection());

        // "2-3" 형식 파싱 -> carNumber=2, doorNumber=3
        int[] carAndDoor = parseCarAndDoor(item.getQuickExitLocation());

        Exit exit = Exit.builder()
                .station(stationOpt.get())
                .carNumber(carAndDoor[0])
                .doorNumber(carAndDoor[1])
                .direction(direction)
                .build();

        exitRepository.save(exit);
    }

    //상행or내선 = 0, 하행or외선 = 1로 변환하는 메서드
    private int parseDirection(String directionStr) {
        if ("상행".equals(directionStr) || "내선".equals(directionStr)) {
            return 0;
        } else if ("하행".equals(directionStr) || "외선".equals(directionStr)) {
            return 1;
        }
        return -1; // 알 수 없음
    }

    //"2-3"등의 정보를 정수배열의 [0],[1]로 각각 나눠서 저장하고 반환.
    private int[] parseCarAndDoor(String locationStr) {
        int[] result = {0, 0}; // [car, door]
        if (locationStr == null || locationStr.isEmpty()) {
            return result;
        }

        try {
            if (locationStr.contains("-")) {
                String[] parts = locationStr.split("-");
                result[0] = Integer.parseInt(parts[0]);
                result[1] = Integer.parseInt(parts[1]);
            } else {
                // "-"가 없는 경우 (예: "2") -> 칸 번호만 있다고 가정하거나 예외 처리
                result[0] = Integer.parseInt(locationStr);
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to parse quick exit location: {}", locationStr);
        }
        return result;
    }
}
