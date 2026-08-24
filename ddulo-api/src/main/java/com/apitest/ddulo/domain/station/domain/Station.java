package com.apitest.ddulo.domain.station.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "station")
@Getter // 엔티티 접근용
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 필수
@AllArgsConstructor // Builder 내부용
@Builder // builder() 생성
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long stationId;

    @Column(nullable = false, unique = true)
    private String stationCode;

    @Column(unique = true)
    private String seoulStationCode;

    @Column(nullable = false)
    private String stationName;

    @Column(nullable = false)
    private String lineName; // "1호선"

    @Column
    private Double latitude; //위도

    @Column
    private Double longitude; //경도

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public void updateCoordinates(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void updateSeoulStationCode(String seoulStationCode) {
        this.seoulStationCode = seoulStationCode;
    }
}

