package com.etf.tracker.dto;

import com.etf.tracker.model.DailyChange;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 每日變動摘要 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyChangeSummaryDto {
    private String etfSymbol;
    private LocalDate date;
    private int totalChanges;
    private int newHoldings;
    private int removedHoldings;
    private int increasedHoldings;
    private int decreasedHoldings;
    private List<DailyChangeDto> changes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyChangeDto {
        private String stockSymbol;
        private String stockName;
        private DailyChange.ChangeType changeType;
        private Long previousShares;
        private Long currentShares;
        private Long sharesChange;
        private Double previousWeight;
        private Double currentWeight;
        private Double weightChange;
    }
}
