package com.etf.tracker.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 每日快照實體
 * <p>
 * 代表特定日期的完整持倉記錄
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
public class DailySnapshot {

    /** 資料日期 */
    private LocalDate date;

    /** 該日所有成分股資料 */
    private List<Holding> holdings;

    /** 成分股總數 */
    private int totalCount;

    /** 總權重 (應接近 100%) */
    private BigDecimal totalWeight;

    /**
     * 預設建構子
     */
    public DailySnapshot() {
        this.holdings = new ArrayList<>();
    }

    /**
     * 全參數建構子
     *
     * @param date        資料日期
     * @param holdings    成分股清單
     * @param totalCount  成分股總數
     * @param totalWeight 總權重百分比
     */
    public DailySnapshot(LocalDate date, List<Holding> holdings, int totalCount, BigDecimal totalWeight) {
        this.date = date;
        this.holdings = holdings != null ? new ArrayList<>(holdings) : new ArrayList<>();
        this.totalCount = totalCount;
        this.totalWeight = totalWeight;
    }

    // Getters

    public LocalDate getDate() {
        return date;
    }

    public List<Holding> getHoldings() {
        return holdings;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public BigDecimal getTotalWeight() {
        return totalWeight;
    }

    // Setters

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setHoldings(List<Holding> holdings) {
        this.holdings = holdings != null ? new ArrayList<>(holdings) : new ArrayList<>();
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public void setTotalWeight(BigDecimal totalWeight) {
        this.totalWeight = totalWeight;
    }

    /**
     * 新增成分股
     *
     * @param holding 成分股
     */
    public void addHolding(Holding holding) {
        if (holding != null) {
            this.holdings.add(holding);
            this.totalCount = this.holdings.size();
        }
    }

    /**
     * 重新計算總數與總權重
     */
    public void recalculate() {
        this.totalCount = this.holdings.size();
        this.totalWeight = this.holdings.stream()
                .map(Holding::getWeight)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DailySnapshot that = (DailySnapshot) o;
        return Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date);
    }

    @Override
    public String toString() {
        return "DailySnapshot{" +
                "date=" + date +
                ", totalCount=" + totalCount +
                ", totalWeight=" + totalWeight +
                '}';
    }

    /**
     * 建立 DailySnapshot 的 Builder
     *
     * @return 新的 Builder 實例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * DailySnapshot Builder 類別
     */
    public static class Builder {
        private LocalDate date;
        private List<Holding> holdings = new ArrayList<>();
        private int totalCount;
        private BigDecimal totalWeight;

        public Builder date(LocalDate date) {
            this.date = date;
            return this;
        }

        public Builder holdings(List<Holding> holdings) {
            this.holdings = holdings != null ? new ArrayList<>(holdings) : new ArrayList<>();
            return this;
        }

        public Builder totalCount(int totalCount) {
            this.totalCount = totalCount;
            return this;
        }

        public Builder totalWeight(BigDecimal totalWeight) {
            this.totalWeight = totalWeight;
            return this;
        }

        public DailySnapshot build() {
            return new DailySnapshot(date, holdings, totalCount, totalWeight);
        }
    }
}
