package com.apitest.ddulo.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.util.Timeout;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        //커넥션 풀 설정
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(50);  // 총 커넥션
        connectionManager.setDefaultMaxPerRoute(20); // 호스트당 최대 커넥션
        
        //타임아웃 설정 : 공공 API 지연 시 우리 서버 영향을 방지
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(5))        // 연결 맺는 시간 5초
                .setResponseTimeout(Timeout.ofSeconds(10))      // 데이터 읽는 시간 10초
                .setConnectionRequestTimeout(Timeout.ofSeconds(5)) // 풀에서 커넥션 꺼내는 시간 5초
                .build();
        
        //클라이언트 생성
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .disableContentCompression()
                .build();
        
        //Spring RestTemplate에 연결
        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        return new RestTemplate(factory);
    }
}
