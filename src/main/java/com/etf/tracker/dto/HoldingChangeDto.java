package com.etf.tracker.dto;

import java.math.BigDecimal;

import com.etf.tracker.model.ChangeType;

/**
 * 持倉變化 DTO
 * <p>
 * 使用 Java Record 實現不可變資料載體
 * </p>
 *
 * @param stockCode   股票代號
 * @param stockName   股票名稱
 * @param changeType  變化類型
 * @param startShares 起始日股數
 * @param endShares   結束日股數
 * @param sharesDiff  增減股數
 * @param changeRatio 變化比例百分比
 * @param startWeight 起始日權重
 * @param endWeight   結束日權重
 * @param weightDiff  權重變化
 * @author ETF Tracker Team
 * @version 1.0.0
 */
public record HoldingChangeDto(
        String stockCode,
        String stockName,
        ChangeType changeType,
        Long startShares,
        Long endShares,
        Long sharesDiff,
        BigDecimal changeRatio,
        BigDecimal startWeight,
        BigDecimal endWeight,
        BigDecimal weightDiff) {
}
