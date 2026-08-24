package com.apitest.ddulo.domain.subway.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RealtimeTrainRowDto {

    private String statnNm;      // 현재역
    private String trainNo;      // 열차번호
    private String recptnDt;     // 수신 시각
    private String updnLine;     // 0: 상행, 1: 하행
    private String trainSttus;   // 0: 진입, 1: 도착, 2: 출발
    private String statnTnm;     // 행선지
}


