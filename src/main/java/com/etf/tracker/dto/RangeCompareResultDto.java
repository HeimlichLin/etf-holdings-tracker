package com.etf.tracker.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 區間比較結果 DTO
 * <p>
 * 包含兩個日期間的所有持倉變化分類
 * </p>
 *
 * @param startDate    起始日期
 * @param endDate      結束日期
 * @param newAdditions 新進增持清單
 * @param removals     剔除減持清單
 * @param increased    增持清單
 * @param decreased    減持清單
 * @param unchanged    不變清單
 * @author ETF Tracker Team
 * @version 1.0.0
 */
public record RangeCompareResultDto(
        LocalDate startDate,
        LocalDate endDate,
        List<HoldingChangeDto> newAdditions,
        List<HoldingChangeDto> removals,
        List<HoldingChangeDto> increased,
        List<HoldingChangeDto> decreased,
        List<HoldingChangeDto> unchanged) {

    /**
     * 取得新進增持數量
     *
     * @return 新進增持數量
     */
    public int newAdditionsCount() {
        return newAdditions != null ? newAdditions.size() : 0;
    }

    /**
     * 取得剔除減持數量
     *
     * @return 剔除減持數量
     */
    public int removalsCount() {
        return removals != null ? removals.size() : 0;
    }

    /**
     * 取得增持數量
     *
     * @return 增持數量
     */
    public int increasedCount() {
        return increased != null ? increased.size() : 0;
    }

    /**
     * 取得減持數量
     *
     * @return 減持數量
     */
    public int decreasedCount() {
        return decreased != null ? decreased.size() : 0;
    }

    /**
     * 取得不變數量
     *
     * @return 不變數量
     */
    public int unchangedCount() {
        return unchanged != null ? unchanged.size() : 0;
    }

    /**
     * 取得所有變化總數
     *
     * @return 所有變化總數
     */
    public int totalChangesCount() {
        return newAdditionsCount() + removalsCount() + increasedCount() + decreasedCount() + unchangedCount();
    }
}
