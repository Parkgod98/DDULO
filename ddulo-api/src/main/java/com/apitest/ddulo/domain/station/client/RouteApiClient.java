package com.apitest.ddulo.domain.station.client;
import com.apitest.ddulo.domain.station.dto.external.openapi.FastPathData;
import com.apitest.ddulo.global.exception.CustomException;
import com.apitest.ddulo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class RouteApiClient {
    private final RestTemplate restTemplate;

    @Value("${seoul.api.url.shortest}")
    private String baseUrl;

    public FastPathData searchRoute(String startStation, String endStation) {
        // 현재 시간 포맷팅 (YYYY-MM-DD HH:mm:ss)
//        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String currentTime = LocalDateTime.now().withHour(10).withMinute(0).withSecond(0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        System.out.println(currentTime);

        // URI 생성 (UriComponentsBuilder 사용 권장 - 인코딩 이슈 해결)
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .pathSegment(startStation, endStation, currentTime)
                .build()
                .encode()
                .toUri();

//        log.info("서울시 API 호출: {}", uri);

        try {
            // 호출 및 매핑
            return restTemplate.getForObject(uri, FastPathData.class);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }
}
