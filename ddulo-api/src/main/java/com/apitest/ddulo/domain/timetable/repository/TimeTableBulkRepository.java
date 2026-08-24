package com.apitest.ddulo.domain.timetable.repository;

import com.apitest.ddulo.domain.timetable.domain.TimeTable;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TimeTableBulkRepository {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void saveAllBatch(List<TimeTable> timeTables) {
        String sql = "INSERT INTO time_table (" +
                "station_id, line_num, fr_code, seoul_station_code, station_name, " +
                "week_tag, in_out_tag, express_yn, train_no, " +
                "arrive_time, left_time, origin_station_name, dest_station_name, " +
                "origin_station_code, dest_station_code" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql,
                timeTables,
                1000, // 배치 사이즈 (한 번에 1000개씩 끊어서 전송)
                (PreparedStatement ps, TimeTable timeTable) -> {
                    // 순서 중요! SQL의 ? 순서대로 매핑
                    ps.setLong(1, timeTable.getStation().getStationId());
                    ps.setString(2, timeTable.getLineNum());
                    ps.setString(3, timeTable.getFrCode());
                    ps.setString(4, timeTable.getSeoulStationCode());
                    ps.setString(5, timeTable.getStationName());
                    ps.setString(6, timeTable.getWeekTag());
                    ps.setString(7, timeTable.getInOutTag());
                    ps.setString(8, timeTable.getExpressYn());
                    ps.setString(9, timeTable.getTrainNo());
                    ps.setString(10, timeTable.getArriveTime());
                    ps.setString(11, timeTable.getLeftTime());
                    ps.setString(12, timeTable.getOriginStationName());
                    ps.setString(13, timeTable.getDestStationName());
                    ps.setString(14, timeTable.getOriginStationCode());
                    ps.setString(15, timeTable.getDestStationCode());
                });
    }
}