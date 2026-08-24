package com.apitest.ddulo.global.utils;

public class StationUtils {
    // '역' 접미사 제거
    public static String removeStationSuffix(String stationName) {
        return stationName.endsWith("역") ? stationName.substring(0, stationName.length() - 1) : stationName;
    }

    // '역' 접미사 추가
    public static String addStationSuffix(String stationName) {
        return stationName.endsWith("역") ? stationName : stationName + "역";
    }

    // 하버사인(Haversine) 공식을 이용한 두 좌표 사이의 거리 계산
    // @return 거리 (미터 단위)
    public static int calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 지구의 반지름 (km)

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distanceKm = R * c;

        // 미터(m) 단위로 반환 (소수점 버림)
        return (int) (distanceKm * 1000);
    }
}
