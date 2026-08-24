package com.apitest.ddulo.global.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DateUtils {

    // Util: 날짜를 요일로(ex: 2026-01-28 -> WED) 변환하는 메서드
    public static String convertDateToDayKey(String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        return date.getDayOfWeek()
                .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                .toUpperCase();
    }

    // Util: 요일을 키값으로(ex: WED -> 1) 변환하는 메서드
    // 평일:1, 토요일:2, 휴일/일요일:3
    public static String convertDayKeyToWeekTag(String dayKey) {
        if(dayKey.equals("SUN")) return "3";
        if(dayKey.equals("SAT")) return "2";
        return "1";
    }

    // Util: 시간을 10분 간격으로 내림 처리하는 메서드(ex: 14:37:21 -> "1430")
    public static String getCurrentTimeKey() {
        LocalTime now = LocalTime.now();

        // 10분 단위로 내림 (Floor) 계산
        // ex: 47분 -> 40분, 03분 -> 00분
        int minute = now.getMinute();
        int roundedMinute = (minute / 10) * 10;

        // 시간을 다시 설정 (초는 00으로)
        LocalTime targetTime = now.withMinute(roundedMinute).withSecond(0).withNano(0);

        // Redis 키 형식(HHmm)으로 변환
        // 예: 14:30:00 -> "1430"
        return targetTime.format(DateTimeFormatter.ofPattern("HHmm"));
    }

    // Util: 시간을 10분 간격으로 올림 처리하는 메서드(ex: 14:37:21 -> "1440")
    public static String getNextTimeKey() {
        LocalTime now = LocalTime.now();

        // 현재 기준 '내림' 분 계산 (이미 있는 로직)
        int minute = now.getMinute();
        int roundedMinute = (minute / 10) * 10;

        // 현재 구간의 시작 시간으로 설정 (ex: 14:32 -> 14:30:00)
        LocalTime currentBucketTime = now.withMinute(roundedMinute).withSecond(0).withNano(0);

        // 10분을 더함 (LocalTime이 알아서 59분 넘어가면 시단위 올려줌)
        LocalTime nextBucketTime = currentBucketTime.plusMinutes(10);

        return nextBucketTime.format(DateTimeFormatter.ofPattern("HHmm"));
    }

    // Util: HHmm -> HH:mm 으로 변환하는 메서드
    public static String formatTime(String hhmm) {
        if (hhmm == null || hhmm.length() != 4) return hhmm;
        return hhmm.substring(0, 2) + ":" + hhmm.substring(2);
    }

    // Util: HH:mm:ss -> HH:mm (10분 단위 반올림)
    public static String roundToNearest10Minutes(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return "00:00";
        }

        try {
            // 1. 문자열을 ':' 기준으로 자름 (예: "24:12:30")
            String[] parts = timeStr.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            // 초(parts[2])는 무시

            // 2. 먼저 24시 이상을 00시로 보정 (예: 25:10 -> 1:10)
            if (hour >= 24) {
                hour = hour % 24;
            }

            // 3. 분 반올림 로직: (분 + 5) / 10 * 10
            int roundedMinute = ((minute + 5) / 10) * 10;

            // 4. 60분이 되면 시간을 1시간 올리고 분을 0으로
            if (roundedMinute == 60) {
                roundedMinute = 0;
                hour++;

                // 시간을 올렸는데 또 24시가 넘으면 다시 보정 (예: 23:55 -> 24:00 -> 00:00)
                if (hour >= 24) {
                    hour = hour % 24;
                }
            }

            // 5. "HH:mm" 포맷으로 조립해서 반환
            return String.format("%02d:%02d", hour, roundedMinute);

        } catch (Exception e) {
            log.warn("Time format conversion failed: {} -> Defaulting to 00:00", timeStr);
            // 실패 시 원본(24:xx:xx)을 보내면 파이썬이 죽으므로, 안전하게 "00:00" 반환
            return "00:00";
        }
    }
}
