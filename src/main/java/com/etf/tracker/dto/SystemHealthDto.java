package com.etf.tracker.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 系統健康狀態 DTO
 * <p>
 * 提供系統健康檢查的結果資訊
 * </p>
 *
 * @param status       整體狀態 (UP/DOWN/DEGRADED)
 * @param components   各元件狀態
 * @param lastFetchAt  最後一次資料抓取時間
 * @param dataFileSize 資料檔案大小 (bytes)
 * @param totalRecords 總記錄數
 * @param checkedAt    檢查時間
 * @author ETF Tracker Team
 * @version 1.0.0
 */
public record SystemHealthDto(
        HealthStatus status,
        Map<String, ComponentHealth> components,
        LocalDateTime lastFetchAt,
        Long dataFileSize,
        Integer totalRecords,
        LocalDateTime checkedAt) {

    /**
     * 健康狀態列舉
     */
    public enum HealthStatus {
        /** 系統正常運作 */
        UP("正常"),
        /** 系統異常 */
        DOWN("異常"),
        /** 系統部分降級 */
        DEGRADED("降級");

        private final String displayName;

        HealthStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 元件健康狀態
     *
     * @param name    元件名稱
     * @param status  元件狀態
     * @param message 狀態訊息
     */
    public record ComponentHealth(
            String name,
            HealthStatus status,
            String message) {
        public static ComponentHealth up(String name) {
            return new ComponentHealth(name, HealthStatus.UP, "正常");
        }

        public static ComponentHealth down(String name, String message) {
            return new ComponentHealth(name, HealthStatus.DOWN, message);
        }
    }

    /**
     * 建立健康的系統狀態
     *
     * @param lastFetchAt  最後抓取時間
     * @param dataFileSize 資料檔案大小
     * @param totalRecords 總記錄數
     * @return 健康的 SystemHealthDto
     */
    public static SystemHealthDto healthy(LocalDateTime lastFetchAt,
            Long dataFileSize,
            Integer totalRecords) {
        return new SystemHealthDto(
                HealthStatus.UP,
                Map.of(
                        "dataFile", ComponentHealth.up("資料檔案"),
                        "scraper", ComponentHealth.up("網頁擷取器")),
                lastFetchAt,
                dataFileSize,
                totalRecords,
                LocalDateTime.now());
    }

    /**
     * 建立降級的系統狀態
     *
     * @param components 各元件狀態
     * @return 降級的 SystemHealthDto
     */
    public static SystemHealthDto degraded(Map<String, ComponentHealth> components) {
        return new SystemHealthDto(
                HealthStatus.DEGRADED,
                components,
                null,
                null,
                null,
                LocalDateTime.now());
    }

    /**
     * 建立異常的系統狀態
     *
     * @param errorMessage 錯誤訊息
     * @return 異常的 SystemHealthDto
     */
    public static SystemHealthDto unhealthy(String errorMessage) {
        return new SystemHealthDto(
                HealthStatus.DOWN,
                Map.of("system", ComponentHealth.down("系統", errorMessage)),
                null,
                null,
                null,
                LocalDateTime.now());
    }

    /**
     * 檢查系統是否健康
     *
     * @return true 如果系統健康
     */
    public boolean isHealthy() {
        return status == HealthStatus.UP;
    }
}
