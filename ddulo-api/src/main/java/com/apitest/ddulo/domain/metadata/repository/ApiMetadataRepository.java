package com.apitest.ddulo.domain.metadata.repository;

import com.apitest.ddulo.domain.metadata.domain.ApiMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApiMetadataRepository
        extends JpaRepository<ApiMetadata, Long> {

    Optional<ApiMetadata> findByApiName(String apiName);
}

