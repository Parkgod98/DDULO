package com.apitest.ddulo.domain.timetable.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Data
public class TimeTableResponse {
    @JsonProperty("SearchSTNTimeTableByIDService")
    private SearchResult result;

    @Data
    public static class SearchResult {
        @JsonProperty("list_total_count")
        private int listTotalCount;

        @JsonProperty("RESULT")
        private ResultMeta resultMeta;

        @JsonProperty("row")
        private List<TimeTableRow> rows;
    }

    @Data
    public static class ResultMeta {
        @JsonProperty("CODE")
        private String code;
        @JsonProperty("MESSAGE")
        private String message;
    }

    @Getter
    @ToString
    public static class TimeTableRow {
        @JsonProperty("LINE_NUM")
        private String lineNum;

        @JsonProperty("FR_CODE")
        private String frCode;

        @JsonProperty("STATION_CD")
        private String stationCd;

        @JsonProperty("STATION_NM")
        private String stationNm;

        @JsonProperty("TRAIN_NO")
        private String trainNo;

        @JsonProperty("ARRIVETIME")
        private String arriveTime;

        @JsonProperty("LEFTTIME")
        private String leftTime;

        @JsonProperty("ORIGINSTATION")
        private String originStation;

        @JsonProperty("DESTSTATION")
        private String destStation;

        @JsonProperty("SUBWAYSNAME")
        private String subwaysName;

        @JsonProperty("SUBWAYENAME")
        private String subwayeName;

        @JsonProperty("WEEK_TAG")
        private String weekTag;

        @JsonProperty("INOUT_TAG")
        private String inOutTag;

        @JsonProperty("FL_FLAG")
        private String flFlag;

        @JsonProperty("DESTSTATION2")
        private String destStation2;

        @JsonProperty("EXPRESS_YN")
        private String expressYn;

        @JsonProperty("BRANCH_LINE")
        private String branchLine;
    }
}
