//package com.apitest.ddulo.domain.subway.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class TrainCongestionScheduler {
//
//    private final TrainCongestionSyncService syncService;
//
//    // 매달 1일 새벽 3시
////    @Scheduled(cron = "0 0 3 1 * ?")
//    @Scheduled(cron = "0 */30 * * * ?") // 테스트용 cron (30분마다)
////    @Scheduled(cron = "0 * * * * ?") // 1분마다
//    public void runMonthlySync() {
//        log.info("🚆 Monthly train congestion sync started");
//        syncService.syncMonthlyCongestion();
//    }
//}
//
