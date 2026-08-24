package com.apitest.ddulo.domain.station.repository;

import com.apitest.ddulo.domain.station.domain.Exit;
import com.apitest.ddulo.domain.station.domain.Station;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExitRepository extends JpaRepository<Exit, Long> {
    List<Exit> findByStationAndDirection(Station station, Integer direction);
}
