package com.etf.tracker.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 持倉變化實體
 * <p>
 * 代表兩個日期間單一成分股的增減變化
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
public class HoldingChange {

    /** 股票代號 */
    private String stockCode;

    /** 股票名稱 */
    private String stockName;

    /** 變化類型 */
    private ChangeType changeType;

    /** 起始日股數 (新進為 null) */
    private Long startShares;

    /** 結束日股數 (剔除為 null) */
    private Long endShares;

    /** 增減股數 */
    private Long sharesDiff;

    /** 變化比例 (百分比) */
    private BigDecimal changeRatio;

    /** 起始日權重 */
    private BigDecimal startWeight;

    /** 結束日權重 */
    private BigDecimal endWeight;

    /** 權重變化 */
    private BigDecimal weightDiff;

    /**
     * 預設建構子
     */
    public HoldingChange() {
    }

    /**
     * 全參數建構子
     */
    public HoldingChange(String stockCode, String stockName, ChangeType changeType,
            Long startShares, Long endShares, Long sharesDiff,
            BigDecimal changeRatio, BigDecimal startWeight,
            BigDecimal endWeight, BigDecimal weightDiff) {
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.changeType = changeType;
        this.startShares = startShares;
        this.endShares = endShares;
        this.sharesDiff = sharesDiff;
        this.changeRatio = changeRatio;
        this.startWeight = startWeight;
        this.endWeight = endWeight;
        this.weightDiff = weightDiff;
    }

    // Getters

    public String getStockCode() {
        return stockCode;
    }

    public String getStockName() {
        return stockName;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public Long getStartShares() {
        return startShares;
    }

    public Long getEndShares() {
        return endShares;
    }

    public Long getSharesDiff() {
        return sharesDiff;
    }

    public BigDecimal getChangeRatio() {
        return changeRatio;
    }

    public BigDecimal getStartWeight() {
        return startWeight;
    }

    public BigDecimal getEndWeight() {
        return endWeight;
    }

    public BigDecimal getWeightDiff() {
        return weightDiff;
    }

    // Setters

    public void setStockCode(String stockCode) {
        this.stockCode = stockCode;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public void setChangeType(ChangeType changeType) {
        this.changeType = changeType;
    }

    public void setStartShares(Long startShares) {
        this.startShares = startShares;
    }

    public void setEndShares(Long endShares) {
        this.endShares = endShares;
    }

    public void setSharesDiff(Long sharesDiff) {
        this.sharesDiff = sharesDiff;
    }

    public void setChangeRatio(BigDecimal changeRatio) {
        this.changeRatio = changeRatio;
    }

    public void setStartWeight(BigDecimal startWeight) {
        this.startWeight = startWeight;
    }

    public void setEndWeight(BigDecimal endWeight) {
        this.endWeight = endWeight;
    }

    public void setWeightDiff(BigDecimal weightDiff) {
        this.weightDiff = weightDiff;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        HoldingChange that = (HoldingChange) o;
        return Objects.equals(stockCode, that.stockCode) &&
                Objects.equals(changeType, that.changeType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stockCode, changeType);
    }

    @Override
    public String toString() {
        return "HoldingChange{" +
                "stockCode='" + stockCode + '\'' +
                ", stockName='" + stockName + '\'' +
                ", changeType=" + changeType +
                ", sharesDiff=" + sharesDiff +
                ", weightDiff=" + weightDiff +
                '}';
    }

    /**
     * 建立 HoldingChange 的 Builder
     *
     * @return 新的 Builder 實例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * HoldingChange Builder 類別
     */
    public static class Builder {
        private String stockCode;
        private String stockName;
        private ChangeType changeType;
        private Long startShares;
        private Long endShares;
        private Long sharesDiff;
        private BigDecimal changeRatio;
        private BigDecimal startWeight;
        private BigDecimal endWeight;
        private BigDecimal weightDiff;

        public Builder stockCode(String stockCode) {
            this.stockCode = stockCode;
            return this;
        }

        public Builder stockName(String stockName) {
            this.stockName = stockName;
            return this;
        }

        public Builder changeType(ChangeType changeType) {
            this.changeType = changeType;
            return this;
        }

        public Builder startShares(Long startShares) {
            this.startShares = startShares;
            return this;
        }

        public Builder endShares(Long endShares) {
            this.endShares = endShares;
            return this;
        }

        public Builder sharesDiff(Long sharesDiff) {
            this.sharesDiff = sharesDiff;
            return this;
        }

        public Builder changeRatio(BigDecimal changeRatio) {
            this.changeRatio = changeRatio;
            return this;
        }

        public Builder startWeight(BigDecimal startWeight) {
            this.startWeight = startWeight;
            return this;
        }

        public Builder endWeight(BigDecimal endWeight) {
            this.endWeight = endWeight;
            return this;
        }

        public Builder weightDiff(BigDecimal weightDiff) {
            this.weightDiff = weightDiff;
            return this;
        }

        public HoldingChange build() {
            return new HoldingChange(stockCode, stockName, changeType,
                    startShares, endShares, sharesDiff,
                    changeRatio, startWeight, endWeight, weightDiff);
        }
    }
}
