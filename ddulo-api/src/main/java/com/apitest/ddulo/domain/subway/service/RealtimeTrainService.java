package com.apitest.ddulo.domain.subway.service;

import com.apitest.ddulo.domain.subway.dto.RealtimeTrainApiResponse;
import com.apitest.ddulo.domain.subway.dto.RealtimeTrainRowDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealtimeTrainService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${seoul.api.url.now}")
    private String baseUrl;

    public List<RealtimeTrainRowDto> getRealtimeTrains(String subwayNm) {

        String url = baseUrl + "/" + subwayNm;

        try {
            String response = restTemplate.getForObject(url, String.class);
            log.info("📦 RAW RESPONSE = {}", response);

            RealtimeTrainApiResponse wrapper =
                    objectMapper.readValue(response, RealtimeTrainApiResponse.class);

            // 데이터 없음
            if (wrapper.getRealtimePositionList() == null
                    || wrapper.getRealtimePositionList().isEmpty()) {
                log.info("🚇 실시간 열차 없음");
                return List.of();
            }

            // 정상
            log.info("🚆 실시간 열차 수: {}",
                    wrapper.getRealtimePositionList().size());

            // TODO: recptnDt 기준 Ghost Train 필터링 (ex. 2~3분 이상 지난 데이터 제거)
            return wrapper.getRealtimePositionList();
        } catch (Exception e) {
            log.error("❌ 실시간 열차 API 호출 실패", e);
            return List.of();
        }
    }

}

