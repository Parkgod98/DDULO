package com.apitest.ddulo.domain.station.service;

import com.apitest.ddulo.domain.station.dto.external.redis.StationDetailData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StationRedisServiceTest {

    @InjectMocks
    private StationRedisService stationRedisService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Redis 조회 성공")
    void getStationDetail_Success() throws Exception {
        // Given
        String stationCode = "221";
        String json = "{\"station\": {\"stationId\": \"221\"}}";
        StationDetailData expectedData = new StationDetailData();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(json);
        when(objectMapper.readValue(json, StationDetailData.class)).thenReturn(expectedData);

        // When
        StationDetailData result = stationRedisService.getStationDetail(stationCode);

        // Then
        assertNotNull(result);
    }
}
