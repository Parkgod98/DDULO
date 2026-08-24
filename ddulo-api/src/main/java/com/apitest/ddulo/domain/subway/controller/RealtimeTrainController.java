//package com.apitest.ddulo.domain.subway.controller;
//
//import com.apitest.ddulo.domain.subway.dto.RealtimeTrainRowDto;
//import com.apitest.ddulo.domain.subway.service.RealtimeTrainService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.List;
//
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("/api/v1/realtime")
//public class RealtimeTrainController {
//
//    private final RealtimeTrainService realtimeTrainService;
//
//    @GetMapping("/trains")
//    public List<RealtimeTrainRowDto> getRealtimeTrains(
//            @RequestParam String subwayNm
//    ) {
//        System.out.println("🔥 realtime controller hit: " + subwayNm);
//        return realtimeTrainService.getRealtimeTrains(subwayNm);
//    }
//}
//
