package com.etf.tracker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * ETF 成分股實體
 * 記錄 ETF 在特定日期的成分股資訊
 */
@Entity
@Table(name = "etf_holdings", indexes = {
    @Index(name = "idx_etf_date", columnList = "etfSymbol, recordDate"),
    @Index(name = "idx_stock_symbol", columnList = "stockSymbol")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtfHolding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ETF 代碼 (例如: ARKK, ARKW)
     */
    @Column(nullable = false)
    private String etfSymbol;

    /**
     * 記錄日期
     */
    @Column(nullable = false)
    private LocalDate recordDate;

    /**
     * 成分股代碼
     */
    @Column(nullable = false)
    private String stockSymbol;

    /**
     * 成分股名稱
     */
    @Column(nullable = false)
    private String stockName;

    /**
     * 持股數量
     */
    private Long shares;

    /**
     * 持股市值
     */
    private Double marketValue;

    /**
     * 持股權重 (百分比)
     */
    private Double weight;
}
