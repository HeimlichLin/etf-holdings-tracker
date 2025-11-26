package com.etf.tracker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * ETF 成分股每日變動記錄
 * 用於追蹤成分股的增減變化
 */
@Entity
@Table(name = "daily_changes", indexes = {
    @Index(name = "idx_change_date", columnList = "etfSymbol, changeDate"),
    @Index(name = "idx_change_type", columnList = "changeType")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ETF 代碼
     */
    @Column(nullable = false)
    private String etfSymbol;

    /**
     * 變動日期
     */
    @Column(nullable = false)
    private LocalDate changeDate;

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
     * 變動類型: NEW (新增), REMOVED (移除), INCREASED (增持), DECREASED (減持)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChangeType changeType;

    /**
     * 前一日持股數量
     */
    private Long previousShares;

    /**
     * 當日持股數量
     */
    private Long currentShares;

    /**
     * 持股變動數量
     */
    private Long sharesChange;

    /**
     * 前一日持股權重
     */
    private Double previousWeight;

    /**
     * 當日持股權重
     */
    private Double currentWeight;

    /**
     * 權重變動
     */
    private Double weightChange;

    /**
     * 變動類型枚舉
     */
    public enum ChangeType {
        NEW,        // 新增成分股
        REMOVED,    // 移除成分股
        INCREASED,  // 增持
        DECREASED   // 減持
    }
}
