package com.apitest.ddulo.domain.subway.repository;

import com.apitest.ddulo.domain.subway.domain.TrainCongestion;
import com.apitest.ddulo.global.common.enums.DayOfWeek;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;

public interface TrainCongestionRepository
        extends JpaRepository<TrainCongestion, Long> {

    @Query("""
        SELECT tc
        FROM TrainCongestion tc
        WHERE tc.station.stationId = :stationId
          AND tc.direction = :direction
          AND tc.dayOfWeek = :dayOfWeek
          AND tc.measuredAt > :currentTime
        ORDER BY tc.measuredAt ASC
    """)
    List<TrainCongestion> findFutureCongestions(
            @Param("stationId") Long stationId,
            @Param("direction") int direction,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("currentTime") LocalTime currentTime,
            Pageable pageable
    );
}
