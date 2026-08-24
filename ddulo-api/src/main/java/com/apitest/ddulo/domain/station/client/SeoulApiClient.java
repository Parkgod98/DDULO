package com.apitest.ddulo.domain.station.client;

import com.apitest.ddulo.domain.station.dto.external.openapi.FastExitData;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class SeoulApiClient {

    @Value("${seoul.api.url.alight}")
    private String alightUrl;

    private final RestTemplate restTemplate;

    public FastExitData fetchExitInfo(int startIndex, int endIndex) {
        String url = String.format("%s/%d/%d", alightUrl, startIndex, endIndex);
        return restTemplate.getForObject(url, FastExitData.class);
    }
}
