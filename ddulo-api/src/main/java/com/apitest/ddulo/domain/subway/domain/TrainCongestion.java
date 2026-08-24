package com.apitest.ddulo.domain.subway.domain;

import com.apitest.ddulo.domain.station.domain.Station;
import com.apitest.ddulo.global.common.BaseTimeEntity;
import com.apitest.ddulo.global.common.enums.DayOfWeek;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(
        name = "train_congestion",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "station_id",
                                "direction",
                                "stat_start_date",
                                "measured_at",
                                "day_of_week"
                        }
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TrainCongestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "congestion_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    private int direction; // 0: 상행/외선, 1: 하행/내선

    @Column(name = "stat_start_date", nullable = false)
    private LocalDate statStartDate;

    @Column(name = "stat_end_date", nullable = false)
    private LocalDate statEndDate;

    @Column(name = "measured_at", nullable = false)
    private LocalTime measuredAt;

    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    /** JSON */
    @Column(name = "congestion_cars", columnDefinition = "json", nullable = false)
    private String congestionCarsJson;
}

