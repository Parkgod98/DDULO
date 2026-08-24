package com.apitest.ddulo.domain.station.client;

import com.apitest.ddulo.domain.station.dto.external.openapi.FastTransferData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransferApiClient {

    private final RestTemplate restTemplate;

    @Value("${data.api.url.transfer}")
    private String apiUrl;

    @Value("${data.api.encoding.key}")
    private String serviceKey;

    public FastTransferData fetchTransferData(int page, int perPage) {
        // 서비스 키가 이미 인코딩되어 있다면 build(true)를 사용하여 이중 인코딩 방지
        URI uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("page", page)
                .queryParam("perPage", perPage)
                .queryParam("serviceKey", serviceKey)
                .build(true) 
                .toUri();

        log.info("Fetching transfer data from API: page={}, perPage={}", page, perPage);
        
        try {
            return restTemplate.getForObject(uri, FastTransferData.class);
        } catch (Exception e) {
            log.error("Failed to fetch transfer data: {}", e.getMessage());
            return null;
        }
    }
}
