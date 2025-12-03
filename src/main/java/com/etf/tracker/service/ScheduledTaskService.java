package com.etf.tracker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.etf.tracker.model.DailySnapshot;

/**
 * 排程任務服務
 * <p>
 * 負責執行定時任務，如每日自動抓取資料
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
@Service
public class ScheduledTaskService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskService.class);

    private final DataFetchService dataFetchService;
    private final StorageService storageService;

    public ScheduledTaskService(DataFetchService dataFetchService, StorageService storageService) {
        this.dataFetchService = dataFetchService;
        this.storageService = storageService;
    }

    /**
     * 每日下午 4 點自動抓取持倉資料
     */
    @Scheduled(cron = "0 0 16 * * ?")
    public void fetchHoldingsDaily() {
        logger.info("開始執行每日自動抓取任務");
        try {
            DailySnapshot snapshot = dataFetchService.fetchLatestHoldings();
            storageService.saveSnapshot(snapshot);
            logger.info("每日自動抓取任務完成: 日期={}", snapshot.getDate());
        } catch (Exception e) {
            logger.error("每日自動抓取任務失敗", e);
        }
    }
}
