package com.apitest.ddulo.domain.metadata.service;

import com.apitest.ddulo.domain.metadata.domain.ApiMetadata;
import com.apitest.ddulo.domain.metadata.repository.ApiMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ApiMetadataService {
    private final ApiMetadataRepository apiMetadataRepository;

    @Transactional(readOnly = true)
    public boolean isApiCallNeeded(String apiName) {
        return apiMetadataRepository.findByApiName(apiName)
                .map(ApiMetadata::isUpdateDue)    // 데이터 있으면 엔티티가 null 체크 및 날짜 계산
                .orElse(true);              // 데이터 없으면 일단 호출해야 데이터가 쌓이니까 true
    }

    // 메타데이터 업데이트 헬퍼메서드
    public void updateMetadata(String apiName) {
        ApiMetadata metadata = apiMetadataRepository.findByApiName(apiName)
                .orElseGet(() -> ApiMetadata.builder()
                        .apiName(apiName)
                        .updateIntervalDay(null)
                        .build());

        metadata.markUpdated();
        apiMetadataRepository.save(metadata);
    }
}
