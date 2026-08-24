package com.apitest.ddulo.domain.path.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PathPredictionResponse {
    // 예측 정보 리스트 (최대 3개)
    private List<PredictionDetail> predictions;

    @Getter
    @Builder
    public static class PredictionDetail {
        private LocalDateTime estimatedBoardingTime; // 예상 탑승 시각
        private LocalDateTime estimatedArrivalTime;  // 예상 도착 시각
        private long waitingTimeSeconds;             // 대기 시간 (초)
        private int boardingProbability;          // 탑승 확률 (%)
    }
}
