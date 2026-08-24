package com.apitest.ddulo.domain.subway.service;

import com.apitest.ddulo.domain.subway.domain.TrainCongestion;
import com.apitest.ddulo.domain.subway.repository.TrainCongestionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TrainCongestionInsertService {

    private final TrainCongestionRepository congestionRepository;

    @Transactional
    public void insertCongestions(List<TrainCongestion> newCongestions) {
        int batchSize = 500;
        for (int i = 0; i < newCongestions.size(); i += batchSize) {
            int end = Math.min(i + batchSize, newCongestions.size());
            congestionRepository.saveAll(newCongestions.subList(i, end));
            congestionRepository.flush();
        }
    }
}

