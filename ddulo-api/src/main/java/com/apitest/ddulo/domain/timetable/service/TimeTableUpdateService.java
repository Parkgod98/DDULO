//package com.apitest.ddulo.domain.timetable.service;
//
//import com.apitest.ddulo.domain.station.domain.Station;
//import com.apitest.ddulo.domain.timetable.client.SeoulTimeTableClient;
//import com.apitest.ddulo.domain.timetable.domain.TimeTable;
//import com.apitest.ddulo.domain.timetable.dto.TimeTableResponse;
//import com.apitest.ddulo.domain.timetable.repository.TimeTableRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Propagation;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.ArrayList;
//import java.util.List;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class TimeTableUpdateService {
//
//    private final TimeTableRepository timeTableRepository;
//    private final SeoulTimeTableClient seoulTimeTableClient;
//
//    //단일 역 동기화 로직
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    public void syncTimeTableForStation(Station station) {
//        String seoulCode = station.getSeoulStationCode();
//
//        // 기존 데이터 삭제
//        timeTableRepository.deleteByStation(station);
//
//        List<TimeTable> timeTablesToSave = new ArrayList<>();
//
//        // 3중 루프 : 요일(1~3) x 방향(1~2)
//        // Day: 1(평일), 2(토요일), 3(휴일)
//        for (int day = 1; day <= 3; day++) {
//            for (int direction = 1; direction <= 2; direction++) {
//                String dayStr = String.valueOf(day);
//                String dirStr = String.valueOf(direction);
//                TimeTableResponse response = seoulTimeTableClient.fetchTimeTable(seoulCode, dayStr, dirStr);
//
//                if (response == null || response.getResult() == null || response.getResult().getRows() == null) continue;
//
//                for (TimeTableResponse.TimeTableRow row : response.getResult().getRows()) {
//                    timeTablesToSave.add(mapToEntity(row, station));
//                }
//            }
//        }
//
//        // 저장
//        if (!timeTablesToSave.isEmpty()) {
//            timeTableRepository.saveAll(timeTablesToSave);
//            log.info("Saved {} timetables for station: {}", timeTablesToSave.size(), station.getStationName());
//        }
//    }
//
//    // 매퍼 메서드: DTO -> Entity
//    private TimeTable mapToEntity(TimeTableResponse.TimeTableRow row, Station station) {
//        return TimeTable.builder()
//                .station(station)
//                .lineNum(row.getLineNum())
//                .frCode(row.getFrCode())
//                .seoulStationCode(row.getStationCd())
//                .stationName(row.getStationNm())
//                .trainNo(row.getTrainNo())
//                .arriveTime(row.getArriveTime())
//                .leftTime(row.getLeftTime())
//                .originStationCode(row.getOriginStation())
//                .destStationCode(row.getDestStation())
//                .originStationName(row.getSubwaysName())
//                .destStationName(row.getSubwayeName())
//                .weekTag(row.getWeekTag())
//                .inOutTag(row.getInOutTag())
//                .flFlag(row.getFlFlag())
//                .destStationCode2(row.getDestStation2())
//                .expressYn(row.getExpressYn())
//                .branchLine(row.getBranchLine())
//                .build();
//    }
//}