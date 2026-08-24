package com.apitest.ddulo.domain.timetable.domain;

import com.apitest.ddulo.domain.station.domain.Station;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "time_table", indexes = {
        @Index(name = "idx_timetable_station_day_direction", columnList = "station_id, weekTag, inOutTag"),
        @Index(name = "idx_timetable_arrival_time", columnList = "arriveTime")
})
public class TimeTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 기존 Station 엔티티와 연결 (FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    // API 제공 원본 데이터 매핑

    @Column(length = 10)
    private String lineNum;      // LINE_NUM: 호선 (ex: 03호선)

    @Column(length = 10)
    private String frCode;       // FR_CODE: 외부코드 (ex: 319)

    @Column(length = 10)
    private String seoulStationCode; // STATION_CD: 전철역코드 (ex: 0309) - Station 엔티티 매핑용 키

    @Column(length = 50)
    private String stationName;  // STATION_NM: 전철역명 (ex: 지축)

    @Column(length = 20)
    private String trainNo;      // TRAIN_NO: 열차번호 (ex: 3012)

    @Column(length = 8)
    private String arriveTime;   // ARRIVETIME: 도착시간 (ex: 05:59:00)

    @Column(length = 8)
    private String leftTime;     // LEFTTIME: 출발시간 (ex: 05:59:30)

    @Column(length = 10)
    private String originStationCode; // ORIGINSTATION: 출발지하철역코드

    @Column(length = 10)
    private String destStationCode;   // DESTSTATION: 도착지하철역코드

    @Column(length = 50)
    private String originStationName; // SUBWAYSNAME: 출발지하철역명

    @Column(length = 50)
    private String destStationName;   // SUBWAYENAME: 도착지하철역명

    @Column(length = 10)
    private String weekTag;      // WEEK_TAG: 요일 (1:평일, 2:토, 3:휴일)

    @Column(length = 10)
    private String inOutTag;     // INOUT_TAG: 상/하행선 (1:상행/내선, 2:하행/외선)

    @Column(length = 10)
    private String flFlag;       // FL_FLAG: 플러그 (거의 빈값)

    @Column(length = 10)
    private String destStationCode2; // DESTSTATION2: 도착역 코드2

    @Column(length = 1)
    private String expressYn;    // EXPRESS_YN: 급행여부 (G:일반, D:급행)

    @Column(length = 50)
    private String branchLine;   // BRANCH_LINE: 지선

    public String getShowTime() {
        // 도착 시간이 '00:00:00'이면 (시작역인 경우) -> 출발 시간을 쓴다.
        if ("00:00:00".equals(this.arriveTime) || this.arriveTime == null) {
            return this.leftTime;
        }

        // 그 외(일반적인 경우) -> 도착 시간을 쓴다.
        return this.arriveTime;
    }
}
