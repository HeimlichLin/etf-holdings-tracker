package com.etf.tracker.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.etf.tracker.model.DailySnapshot;

/**
 * 儲存服務介面
 * <p>
 * 定義資料儲存的標準操作，可由 Excel 或 Google Sheets 實作
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
public interface StorageService {

    /**
     * 儲存每日快照
     *
     * @param snapshot 每日快照
     */
    void saveSnapshot(DailySnapshot snapshot);

    /**
     * 取得指定日期的快照
     *
     * @param date 日期
     * @return 快照，如果不存在則為空
     */
    Optional<DailySnapshot> getSnapshot(LocalDate date);

    /**
     * 取得最新的快照
     *
     * @return 最新快照，如果沒有資料則為空
     */
    Optional<DailySnapshot> getLatestSnapshot();

    /**
     * 取得所有可用日期（降序排列）
     *
     * @return 日期清單
     */
    List<LocalDate> getAvailableDates();

    /**
     * 計算指定日期之前的記錄數（用於清理預覽）
     *
     * @param cutoffDate 截止日期
     * @return 符合條件的記錄數
     */
    int countRecordsBefore(LocalDate cutoffDate);

    /**
     * 取得總記錄數（所有日期的成分股總筆數）
     *
     * @return 總記錄數
     */
    int getTotalRecordCount();

    /**
     * 刪除指定日期之前的資料
     *
     * @param cutoffDate 截止日期
     * @return 刪除的記錄數
     */
    int deleteDataBefore(LocalDate cutoffDate);

    /**
     * 判斷目前是否為唯讀模式
     * <p>
     * 當資料來源是 Google Sheets 時，為唯讀模式，不允許編輯
     * </p>
     *
     * @return true 表示唯讀，false 表示可編輯
     */
    default boolean isReadOnly() {
        return false;
    }

    /**
     * 取得資料來源資訊
     *
     * @return 資料來源描述
     */
    default String getDataSourceInfo() {
        return "本地 Excel";
    }
}
