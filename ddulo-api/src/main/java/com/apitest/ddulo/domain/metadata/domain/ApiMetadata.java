package com.apitest.ddulo.domain.metadata.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_metadata")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ApiMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long apiId;

    @Column(nullable = false, unique = true)
    private String apiName;

    private LocalDateTime updatedAt;

    private Integer updateIntervalDay; // 예: 7일

    //업데이트가 가능한지 여부
    public boolean isUpdateDue() {
        if (updateIntervalDay == null) {
            return false;
        }

        if (updatedAt == null) {
            return true;
        }

        return updatedAt.plusDays(updateIntervalDay)
                .isBefore(LocalDateTime.now());
    }

    public void markUpdated() {
        this.updatedAt = LocalDateTime.now();
    }
}
