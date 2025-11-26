package com.etf.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ETF 成分股 DTO
 * 用於資料傳輸的輕量級物件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldingDto {
    private String stockSymbol;
    private String stockName;
    private Long shares;
    private Double marketValue;
    private Double weight;
}
