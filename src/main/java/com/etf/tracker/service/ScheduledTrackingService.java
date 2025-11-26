package com.etf.tracker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 排程服務
 * 每日自動執行 ETF 成分股追蹤
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTrackingService {

    private final EtfHoldingsTrackingService trackingService;

    @Value("${etf.tracked-symbols:ARKK,ARKW,ARKQ,ARKG,ARKF}")
    private String trackedSymbols;

    /**
     * 每日執行追蹤任務
     * 預設在美東時間 18:00 (交易日收盤後) 執行
     * cron 格式: 秒 分 時 日 月 週
     */
    @Scheduled(cron = "${etf.scheduler.cron:0 0 22 * * MON-FRI}")
    public void dailyTrackingTask() {
        log.info("開始執行每日 ETF 成分股追蹤任務");

        List<String> symbols = List.of(trackedSymbols.split(","));
        
        for (String symbol : symbols) {
            try {
                String trimmedSymbol = symbol.trim();
                log.info("追蹤 ETF: {}", trimmedSymbol);
                trackingService.updateAndTrack(trimmedSymbol);
            } catch (Exception e) {
                log.error("追蹤 {} 時發生錯誤: {}", symbol, e.getMessage(), e);
            }
        }

        log.info("每日追蹤任務完成");
    }

    /**
     * 每週清理過期資料
     */
    @Scheduled(cron = "${etf.scheduler.cleanup-cron:0 0 0 * * SUN}")
    public void weeklyCleanupTask() {
        log.info("開始執行每週資料清理任務");
        try {
            trackingService.cleanupOldData();
            log.info("每週清理任務完成");
        } catch (Exception e) {
            log.error("清理任務發生錯誤: {}", e.getMessage(), e);
        }
    }
}
