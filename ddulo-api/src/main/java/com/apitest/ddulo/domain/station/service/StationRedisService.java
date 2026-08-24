package com.apitest.ddulo.domain.station.service;

import com.apitest.ddulo.domain.station.dto.external.redis.StationDetailData;
import com.apitest.ddulo.global.exception.CustomException;
import com.apitest.ddulo.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

import static com.apitest.ddulo.global.utils.DateUtils.convertDateToDayKey;
import static com.apitest.ddulo.global.utils.DateUtils.getCurrentTimeKey;

@Slf4j
@Service
@RequiredArgsConstructor
public class StationRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;

    // Redis에서 역 상세정보(양 방향으로 인접한 열차 상태 3개씩 조회) 조회
    public StationDetailData getStationDetail(String stationCode) {
        // Redis Key 생성: stat:near:{stationId}:{요일}:{시간}
        String redisKey = String.format(
                "stat:near:%s:%s:%s",
                stationCode,
                convertDateToDayKey(String.valueOf(LocalDate.now())),
                getCurrentTimeKey()
        );

//        Object rawData = redisTemplate.opsForValue().get(redisKey);
        // 문자열(JSON String) 상태 그대로 가져오기
        String jsonValue = stringRedisTemplate.opsForValue().get(redisKey);

        if (jsonValue == null)
            return createEmptyStationData(stationCode);

        try {
//            return objectMapper.readValue(rawData.toString(), StationDetailData.class);
            return objectMapper.readValue(jsonValue, StationDetailData.class);
        } catch (Exception e) {
            log.error("Redis 데이터 변환 실패 - key: {}, error: {}", redisKey, e.getMessage(), e);
            throw new CustomException(ErrorCode.DATA_CONVERSION_ERROR);
        }
    }

    // 빈 껍데기 객체 생성 헬퍼 메서드
    private StationDetailData createEmptyStationData(String stationCode) {
        return StationDetailData.builder()
                .station(new StationDetailData.StationInfo(stationCode, "", "")) // 역 코드는 유지
                .upBound(List.of())     // 빈 리스트 [] (프론트 에러 방지 핵심)
                .downBound(List.of())   // 빈 리스트 []
                .context(new StationDetailData.StationContext(
                        LocalDate.now().toString(),
                        "UNKNOWN",
                        "0000",
                        "EMPTY_DATA"
                ))
                .build();
    }



}
