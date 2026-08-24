package com.apitest.ddulo.global.utils;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class SubwayUtils {
    public static String getDirectionName(int direction, String lineName) {
        if (lineName == null) return direction == 0 ? "상행" : "하행";

        // 지선 먼저 필터링 (성수, 신도림 지선 -> 상행/하행)
        if (lineName.contains("성수지선") || lineName.contains("신도림지선")) {
            return direction == 0 ? "상행" : "하행";
        }

        // 2호선 본선만 (내선/외선)
        if (lineName.contains("2호선")) {
            return direction == 0 ? "내선" : "외선";
        }

        // 6호선 포함 나머지 모든 노선 (상행/하행)
        // 응암 순환 구간도 여기서 '상행/하행'으로 처리됨
        return direction == 0 ? "상행" : "하행";
    }

    // 방향을 숫자 문자열로 변환
    public static String getDirectionNumber(String direction) {
        if(direction.equals("내선") || direction.equals("상행"))
            return "1";
        if(direction.equals("외선") || direction.equals("하행"))
            return "2";
        return null;
    }

    // 방향을 정수 숫자로 변환
    public static int getDirectionNumberInt(String direction) {
        if(direction.equals("내선") || direction.equals("상행"))
            return 0;
        if(direction.equals("외선") || direction.equals("하행"))
            return 1;
        return -1;
    }
}
