package com.apitest.ddulo.domain.station.dto.external.openapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FastExitData {

    private Response response;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {
        private Body body;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        private Items items;
        
        @JsonProperty("totalCount")
        private int totalCount;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items {
        private List<Item> item;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        @JsonProperty("stnNo")
        private String stationCode;

        @JsonProperty("stnNm")
        private String stationName;

        @JsonProperty("lineNm")
        private String lineName;

        @JsonProperty("upbdnbSe")
        private String direction; // "상행", "하행"

        @JsonProperty("qckgffVhclDoorNo")
        private String quickExitLocation; // 예: "2-3"
    }
}
