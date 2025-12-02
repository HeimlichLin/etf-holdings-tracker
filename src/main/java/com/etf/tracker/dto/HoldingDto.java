package com.etf.tracker.dto;

import java.math.BigDecimal;

/**
 * 成分股 DTO
 * <p>
 * 使用 Java Record 實現不可變資料載體
 * </p>
 *
 * @param stockCode 股票代號
 * @param stockName 股票名稱
 * @param shares    持股股數
 * @param weight    持股權重百分比
 * @author ETF Tracker Team
 * @version 1.0.0
 */
public record HoldingDto(
        String stockCode,
        String stockName,
        Long shares,
        BigDecimal weight) {
}
