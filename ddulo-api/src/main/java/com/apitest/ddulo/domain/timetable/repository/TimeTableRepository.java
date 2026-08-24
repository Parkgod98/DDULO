package com.apitest.ddulo.domain.timetable.repository;

import com.apitest.ddulo.domain.station.domain.Station;
import com.apitest.ddulo.domain.timetable.domain.TimeTable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TimeTableRepository extends JpaRepository<TimeTable, Long> {
    // 특정 역의 시간표를 삭제 (업데이트 전 초기화용)
    @Modifying
    @Query("DELETE FROM TimeTable t WHERE t.station = :station")
    void deleteByStation(@Param("station") Station station);

    @Query("SELECT t FROM TimeTable t " +
            "WHERE t.frCode = :stationCode " +
            "AND t.weekTag = :weekTag " +
            "AND t.inOutTag = :direction " +
            "AND t.leftTime >= :currentTime " + // 현재 시간 포함/이후
            "ORDER BY t.leftTime ASC")
    List<TimeTable> findNextTrains(
            @Param("stationCode") String stationCode,
            @Param("weekTag") String weekTag,
            @Param("direction") String direction,
            @Param("currentTime") String currentTime,
            Pageable pageable
    );
}
