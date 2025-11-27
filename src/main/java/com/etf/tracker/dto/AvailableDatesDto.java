package com.etf.tracker.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 可查詢日期 DTO
 * <p>
 * 提供系統中可查詢的日期範圍資訊
 * </p>
 *
 * @param availableDates 所有可查詢日期清單（降序排列）
 * @param earliestDate   最早日期
 * @param latestDate     最新日期
 * @param totalDays      總天數
 * @author ETF Tracker Team
 * @version 1.0.0
 */
public record AvailableDatesDto(
        List<LocalDate> availableDates,
        LocalDate earliestDate,
        LocalDate latestDate,
        int totalDays) {

    /**
     * 建立空的可查詢日期 DTO
     *
     * @return 空的 AvailableDatesDto
     */
    public static AvailableDatesDto empty() {
        return new AvailableDatesDto(List.of(), null, null, 0);
    }

    /**
     * 從日期清單建立 DTO
     *
     * @param dates 日期清單（應為降序排列）
     * @return AvailableDatesDto
     */
    public static AvailableDatesDto from(List<LocalDate> dates) {
        if (dates == null || dates.isEmpty()) {
            return empty();
        }

        // 假設 dates 已排序為降序
        LocalDate latest = dates.get(0);
        LocalDate earliest = dates.get(dates.size() - 1);

        return new AvailableDatesDto(dates, earliest, latest, dates.size());
    }

    /**
     * 檢查是否有可用資料
     *
     * @return true 如果有可用日期
     */
    public boolean hasData() {
        return totalDays > 0;
    }

    /**
     * 檢查指定日期是否可查詢
     *
     * @param date 要檢查的日期
     * @return true 如果日期可查詢
     */
    public boolean isDateAvailable(LocalDate date) {
        return availableDates != null && availableDates.contains(date);
    }
}
