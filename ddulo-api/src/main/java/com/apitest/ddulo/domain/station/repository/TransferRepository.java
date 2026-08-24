package com.apitest.ddulo.domain.station.repository;

import com.apitest.ddulo.domain.station.domain.Station;
import com.apitest.ddulo.domain.station.domain.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
    Optional<Transfer> findByFromStationAndToStation(Station fromStation, Station toStation);
}
