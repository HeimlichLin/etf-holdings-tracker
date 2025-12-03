package com.etf.tracker.service;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.etf.tracker.config.AppConfig;
import com.etf.tracker.dto.CleanupResultDto;

/**
 * 資料清理服務
 * <p>
 * 負責清理超過保留期限的歷史資料（預設 90 天）
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
@Service
public class DataCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(DataCleanupService.class);

    private final StorageService storageService;
    private final AppConfig appConfig;

    public DataCleanupService(StorageService storageService, AppConfig appConfig) {
        this.storageService = storageService;
        this.appConfig = appConfig;
    }

    /**
     * 使用預設保留天數清理舊資料
     *
     * @return 清理結果
     */
    public CleanupResultDto cleanupOldData() {
        int retentionDays = getDefaultRetentionDays();
        return cleanupOldData(retentionDays);
    }

    /**
     * 使用指定保留天數清理舊資料
     *
     * @param daysToKeep 保留天數
     * @return 清理結果
     * @throws IllegalArgumentException 如果保留天數為負數
     */
    public CleanupResultDto cleanupOldData(int daysToKeep) {
        if (daysToKeep < 0) {
            throw new IllegalArgumentException("保留天數不可為負數: " + daysToKeep);
        }

        // 保留 N 天 = 刪除 N 天前及更早的資料
        // 例如：保留 1 天 = 只保留今天，刪除昨天之前的所有資料
        // 例如：保留 90 天 = 保留最近 90 天，刪除 90 天前及更早的資料
        LocalDate cutoffDate = LocalDate.now().minusDays(daysToKeep);
        logger.info("開始清理舊資料，保留天數: {}，截止日期: {}", daysToKeep, cutoffDate);

        try {
            // 執行刪除
            int deletedCount = storageService.deleteDataBefore(cutoffDate);

            // 取得剩餘資料統計
            List<LocalDate> remainingDates = storageService.getAvailableDates();
            int remainingCount = remainingDates.size();

            logger.info("清理完成: 刪除 {} 筆，剩餘 {} 筆", deletedCount, remainingCount);

            return CleanupResultDto.success(deletedCount, cutoffDate, remainingCount, daysToKeep);

        } catch (Exception e) {
            logger.error("清理資料失敗: {}", e.getMessage(), e);
            return CleanupResultDto.failure("清理資料失敗: " + e.getMessage());
        }
    }

    /**
     * 確認並執行清理
     * <p>
     * 此方法要求明確確認才會執行清理操作
     * </p>
     *
     * @param daysToKeep 保留天數
     * @param confirmed  是否已確認
     * @return 清理結果
     */
    public CleanupResultDto confirmAndCleanup(int daysToKeep, boolean confirmed) {
        if (!confirmed) {
            logger.warn("清理操作未經確認，取消執行");
            return CleanupResultDto.failure("請確認刪除操作");
        }
        return cleanupOldData(daysToKeep);
    }

    /**
     * 預覽將被清理的資料
     * <p>
     * 不實際刪除，僅回傳將被刪除的日期清單
     * </p>
     *
     * @param daysToKeep 保留天數
     * @return 將被刪除的日期清單
     */
    public List<LocalDate> previewCleanup(int daysToKeep) {
        LocalDate cutoffDate = LocalDate.now().minusDays(daysToKeep);
        List<LocalDate> allDates = storageService.getAvailableDates();

        List<LocalDate> toBeDeleted = allDates.stream()
                .filter(date -> date.isBefore(cutoffDate))
                .toList();

        logger.debug("預覽清理: 將刪除 {} 筆資料（截止日期: {}）", toBeDeleted.size(), cutoffDate);
        return toBeDeleted;
    }

    /**
     * 取得清理統計資訊
     *
     * @return 清理統計
     */
    public CleanupStatistics getCleanupStatistics() {
        List<LocalDate> allDates = storageService.getAvailableDates();

        if (allDates.isEmpty()) {
            return new CleanupStatistics(0, 0, 0, null, null);
        }

        int totalRecords = allDates.size();
        LocalDate cutoffDate = LocalDate.now().minusDays(getDefaultRetentionDays());

        long expiredCount = allDates.stream()
                .filter(date -> date.isBefore(cutoffDate))
                .count();

        int expiredRecords = (int) expiredCount;
        int validRecords = totalRecords - expiredRecords;

        // 日期已按降序排列，最後一個是最舊的
        LocalDate newestDate = allDates.get(0);
        LocalDate oldestDate = allDates.get(allDates.size() - 1);

        return new CleanupStatistics(totalRecords, expiredRecords, validRecords, oldestDate, newestDate);
    }

    /**
     * 取得預設保留天數
     *
     * @return 預設保留天數
     */
    public int getDefaultRetentionDays() {
        return appConfig.getData().getRetentionDays();
    }

    /**
     * 清理統計資訊
     *
     * @param totalRecords   總記錄數
     * @param expiredRecords 過期記錄數
     * @param validRecords   有效記錄數
     * @param oldestDate     最舊日期
     * @param newestDate     最新日期
     */
    public record CleanupStatistics(
            int totalRecords,
            int expiredRecords,
            int validRecords,
            LocalDate oldestDate,
            LocalDate newestDate) {

        /**
         * 是否有過期資料
         *
         * @return true 如果有過期資料
         */
        public boolean hasExpiredData() {
            return expiredRecords > 0;
        }

        /**
         * 計算資料跨越天數
         *
         * @return 資料跨越天數，如果無資料則為 0
         */
        public int getDataSpanDays() {
            if (oldestDate == null || newestDate == null) {
                return 0;
            }
            return (int) java.time.temporal.ChronoUnit.DAYS.between(oldestDate, newestDate);
        }
    }
}
