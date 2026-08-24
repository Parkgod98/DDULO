package com.apitest.ddulo.domain.path.service;

import com.apitest.ddulo.domain.path.dto.external.redis.PathFullData;
import com.apitest.ddulo.domain.path.dto.external.redis.PathPredictionData;
import com.apitest.ddulo.global.exception.CustomException;
import com.apitest.ddulo.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

import static com.apitest.ddulo.global.utils.DateUtils.convertDateToDayKey;
import static com.apitest.ddulo.global.utils.DateUtils.getCurrentTimeKey;

@Slf4j
@Service
@RequiredArgsConstructor
public class PathRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // 전체 경로 및 상세 정보 조회 (path:full)
    public PathFullData getFullPath(String startId, String endId) {
        String redisKey = buildRedisKey("path:full", startId, endId);
        
        Object rawData = redisTemplate.opsForValue().get(redisKey);
        
        if (rawData == null) return null;

        try {
            return objectMapper.readValue(rawData.toString(), PathFullData.class);
        } catch (Exception e) {
            log.error("Redis 데이터 변환 실패 - key: {}, error: {}", redisKey, e.getMessage(), e);
            throw new CustomException(ErrorCode.DATA_CONVERSION_ERROR);
        }
    }

    // 경로 예측 요약 정보 조회 (path:pred)
    public PathPredictionData getPathPrediction(String startId, String endId) {
        String redisKey = buildRedisKey("path:pred", startId, endId);

        Object rawData = redisTemplate.opsForValue().get(redisKey);

        if (rawData == null) return null;

        try {
            return objectMapper.readValue(rawData.toString(), PathPredictionData.class);
        } catch (Exception e) {
            log.error("Redis 데이터 변환 실패 - key: {}, error: {}", redisKey, e.getMessage(), e);
            throw new CustomException(ErrorCode.DATA_CONVERSION_ERROR);
        }
    }

    /**
     * Redis 키를 생성하는 헬퍼 메서드
     * 포맷: prefix:startId:endId:dayKey:timeKey
     */
    private String buildRedisKey(String prefix, String startId, String endId) {
        String dayKey = convertDateToDayKey(String.valueOf(LocalDate.now()));
        String timeKey = getCurrentTimeKey();
        
        return String.format("%s:%s:%s:%s:%s", prefix, startId, endId, dayKey, timeKey);
    }
}
