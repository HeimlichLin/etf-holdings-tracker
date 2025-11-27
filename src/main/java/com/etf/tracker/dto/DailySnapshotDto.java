package com.etf.tracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 每日快照 DTO
 * <p>
 * 使用 Java Record 實現不可變資料載體
 * </p>
 *
 * @param date        資料日期
 * @param holdings    成分股清單
 * @param totalCount  成分股總數
 * @param totalWeight 總權重百分比
 * @author ETF Tracker Team
 * @version 1.0.0
 */
public record DailySnapshotDto(
        LocalDate date,
        List<HoldingDto> holdings,
        int totalCount,
        BigDecimal totalWeight) {
}
