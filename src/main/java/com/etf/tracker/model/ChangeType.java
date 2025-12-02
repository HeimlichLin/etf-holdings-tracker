package com.etf.tracker.model;

/**
 * 持倉變化類型列舉
 * <p>
 * 定義兩個日期間成分股的變化類型
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
public enum ChangeType {

    /** 新進增持 - 起始日不存在，結束日新增 */
    NEW_ADDITION("新進"),

    /** 剔除減持 - 起始日存在，結束日移除 */
    REMOVED("剔除"),

    /** 增持 - 股數增加 */
    INCREASED("增持"),

    /** 減持 - 股數減少 */
    DECREASED("減持"),

    /** 不變 - 股數相同 */
    UNCHANGED("不變");

    private final String displayName;

    ChangeType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 取得顯示名稱
     *
     * @return 中文顯示名稱
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 判斷是否為正向變化（新進或增持）
     *
     * @return true 如果是正向變化
     */
    public boolean isPositive() {
        return this == NEW_ADDITION || this == INCREASED;
    }

    /**
     * 判斷是否為負向變化（剔除或減持）
     *
     * @return true 如果是負向變化
     */
    public boolean isNegative() {
        return this == REMOVED || this == DECREASED;
    }

    /**
     * 判斷是否為無變化
     *
     * @return true 如果無變化
     */
    public boolean isUnchanged() {
        return this == UNCHANGED;
    }
}
