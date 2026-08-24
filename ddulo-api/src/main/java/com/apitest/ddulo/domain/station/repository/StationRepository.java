package com.apitest.ddulo.domain.station.repository;

import com.apitest.ddulo.domain.station.domain.Station;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StationRepository
        extends JpaRepository<Station, Long> {

    boolean existsByStationCode(String stationCode);

    Optional<Station> findByStationCode(String stationCode);

    List<Station> findByStationNameContaining(String keyword);

    Page<Station> findAll(Pageable pageable); // 페이지 단위 조회

    Optional<Station> findFirstByStationNameAndLineName(String stationName, String lineName);

    @Query("SELECT s.stationCode FROM Station s WHERE s.stationName = :stationName AND s.lineName = :lineName")
    Optional<String> findStationCodeByNameAndLine(
            @Param("stationName") String stationName,
            @Param("lineName") String lineName
    );

    Optional<Station> findStationByStationNameAndLineName(String stationName, String lineName);
}
