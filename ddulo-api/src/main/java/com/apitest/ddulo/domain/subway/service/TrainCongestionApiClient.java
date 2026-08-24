package com.apitest.ddulo.domain.subway.service;

import com.apitest.ddulo.domain.subway.dto.PuzzleTrainCongestionResponseDto;
import com.apitest.ddulo.global.common.enums.DayOfWeek;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class TrainCongestionApiClient {

    @Value("${sk.api.key}")
    private String apiKey;

    @Value("${sk.api.url.subway.congestion}")
    private String congestionUrl;

    private final RestTemplate restTemplate;

    public PuzzleTrainCongestionResponseDto fetchCongestion(String stationCode, DayOfWeek dow, int hour) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("appkey", apiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = congestionUrl + "/" + stationCode
                + "?dow=" + dow.name()
                + "&hh=" + String.format("%02d", hour);

        try {
            ResponseEntity<PuzzleTrainCongestionResponseDto> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            entity,
                            PuzzleTrainCongestionResponseDto.class
                    );

            return response.getBody();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // 404나 400 등 HTTP 에러 무시하고 null 반환
            System.out.println("⚠️ 데이터 없음: stationCode=" + stationCode);
            return null;
        }
    }
}


