package com.etf.tracker.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 成分股實體
 * <p>
 * 代表 ETF 中的單一持股資訊
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
public class Holding {

    /** 股票代號 (唯一識別碼) */
    private String stockCode;

    /** 股票名稱 */
    private String stockName;

    /** 持股股數 */
    private Long shares;

    /** 持股權重 (百分比) */
    private BigDecimal weight;

    /**
     * 預設建構子
     */
    public Holding() {
    }

    /**
     * 全參數建構子
     *
     * @param stockCode 股票代號
     * @param stockName 股票名稱
     * @param shares    持股股數
     * @param weight    持股權重百分比
     */
    public Holding(String stockCode, String stockName, Long shares, BigDecimal weight) {
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.shares = shares;
        this.weight = weight;
    }

    // Getters

    public String getStockCode() {
        return stockCode;
    }

    public String getStockName() {
        return stockName;
    }

    public Long getShares() {
        return shares;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    // Setters

    public void setStockCode(String stockCode) {
        this.stockCode = stockCode;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public void setShares(Long shares) {
        this.shares = shares;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    // equals & hashCode (以 stockCode 為主鍵)

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Holding holding = (Holding) o;
        return Objects.equals(stockCode, holding.stockCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stockCode);
    }

    @Override
    public String toString() {
        return "Holding{" +
                "stockCode='" + stockCode + '\'' +
                ", stockName='" + stockName + '\'' +
                ", shares=" + shares +
                ", weight=" + weight +
                '}';
    }

    /**
     * 建立 Holding 的 Builder
     *
     * @return 新的 Builder 實例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Holding Builder 類別
     */
    public static class Builder {
        private String stockCode;
        private String stockName;
        private Long shares;
        private BigDecimal weight;

        public Builder stockCode(String stockCode) {
            this.stockCode = stockCode;
            return this;
        }

        public Builder stockName(String stockName) {
            this.stockName = stockName;
            return this;
        }

        public Builder shares(Long shares) {
            this.shares = shares;
            return this;
        }

        public Builder weight(BigDecimal weight) {
            this.weight = weight;
            return this;
        }

        public Holding build() {
            return new Holding(stockCode, stockName, shares, weight);
        }
    }
}
