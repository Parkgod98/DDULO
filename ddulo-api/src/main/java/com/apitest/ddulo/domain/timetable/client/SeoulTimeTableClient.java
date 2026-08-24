//package com.apitest.ddulo.domain.timetable.client;
//
//import com.apitest.ddulo.domain.timetable.dto.TimeTableResponse;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//import org.springframework.web.client.RestTemplate;
//import org.springframework.web.util.UriComponentsBuilder;
//
//import java.net.URI;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class SeoulTimeTableClient {
//
//    @Value("${seoul.api.url.timetable}")
//    private String timeTableBaseUrl;
//
//    private final RestTemplate restTemplate;
//
//    //서울시 API 호출해서 시간표 데이터 가져오기
//    public TimeTableResponse fetchTimeTable(String seoulStationCode, String dayType, String direction) {
//        // UriComponentsBuilder가 알아서 '/'를 처리해주므로 안전
//        URI uri = UriComponentsBuilder.fromHttpUrl(timeTableBaseUrl)
//                .pathSegment(seoulStationCode)  // 역 코드 (0309)
//                .pathSegment(dayType)           // 요일 (1,2,3)
//                .pathSegment(direction)         // 방향 (1,2)
//                .build()
//                .toUri();
//
//        // log.debug("Fetching URL: {}", uri);
//
//        try {
//            return restTemplate.getForObject(uri, TimeTableResponse.class);
//        } catch (Exception e) {
//            log.error("시간표 API 호출 실패 - stationCode: {}, error: {}", seoulStationCode, e.getMessage());
//            return null;
//        }
//    }
//
//}
