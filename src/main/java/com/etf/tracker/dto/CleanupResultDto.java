package com.etf.tracker.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 清理結果 DTO
 * <p>
 * 提供過期資料清理操作的結果資訊
 * </p>
 *
 * @param success          操作是否成功
 * @param deletedRecords   刪除的記錄數量
 * @param cutoffDate       清理截止日期（此日期之前的資料被刪除）
 * @param remainingRecords 剩餘記錄數量
 * @param remainingDays    剩餘天數
 * @param executedAt       執行時間
 * @param message          操作訊息
 * @author ETF Tracker Team
 * @version 1.0.0
 */
public record CleanupResultDto(
        boolean success,
        int deletedRecords,
        LocalDate cutoffDate,
        int remainingRecords,
        int remainingDays,
        LocalDateTime executedAt,
        String message) {

    /**
     * 建立成功的清理結果
     *
     * @param deletedRecords   刪除的記錄數量
     * @param cutoffDate       清理截止日期
     * @param remainingRecords 剩餘記錄數量
     * @param remainingDays    剩餘天數
     * @return 成功的 CleanupResultDto
     */
    public static CleanupResultDto success(int deletedRecords, LocalDate cutoffDate,
            int remainingRecords, int remainingDays) {
        String message = deletedRecords > 0
                ? String.format("已清理 %d 筆過期資料", deletedRecords)
                : "無需清理，所有資料皆在保留期限內";

        return new CleanupResultDto(
                true,
                deletedRecords,
                cutoffDate,
                remainingRecords,
                remainingDays,
                LocalDateTime.now(),
                message);
    }

    /**
     * 建立失敗的清理結果
     *
     * @param errorMessage 錯誤訊息
     * @return 失敗的 CleanupResultDto
     */
    public static CleanupResultDto failure(String errorMessage) {
        return new CleanupResultDto(
                false,
                0,
                null,
                0,
                0,
                LocalDateTime.now(),
                errorMessage);
    }

    /**
     * 檢查是否有資料被刪除
     *
     * @return true 如果有資料被刪除
     */
    public boolean hasDeletedData() {
        return deletedRecords > 0;
    }
}
