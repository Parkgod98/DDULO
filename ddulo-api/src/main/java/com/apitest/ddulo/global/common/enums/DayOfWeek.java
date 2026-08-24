package com.apitest.ddulo.global.common.enums;

import java.util.Arrays;

public enum DayOfWeek {
    MON(1), TUE(2), WED(3), THU(4), FRI(5), SAT(6), SUN(7);

    private final int code;

    DayOfWeek(int code) { this.code = code; }
    public int getCode() { return code; }

    public static DayOfWeek fromCode(int code) {
        return Arrays.stream(values())
                .filter(d -> d.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid DayOfWeek code: " + code));
    }
}
