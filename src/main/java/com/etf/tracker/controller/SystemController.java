package com.etf.tracker.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.etf.tracker.config.AppConfig;
import com.etf.tracker.dto.ApiResponse;
import com.etf.tracker.dto.SystemHealthDto;
import com.etf.tracker.dto.SystemHealthDto.ComponentHealth;
import com.etf.tracker.dto.SystemHealthDto.HealthStatus;
import com.etf.tracker.model.DailySnapshot;
import com.etf.tracker.service.DataCleanupService;
import com.etf.tracker.service.StorageService;

/**
 * 系統控制器
 * <p>
 * 提供系統狀態檢查與健康檢測 API
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/system")
public class SystemController {

    private static final Logger logger = LoggerFactory.getLogger(SystemController.class);

    private final StorageService storageService;
    private final DataCleanupService dataCleanupService;
    private final AppConfig appConfig;

    public SystemController(StorageService storageService,
            DataCleanupService dataCleanupService,
            AppConfig appConfig) {
        this.storageService = storageService;
        this.dataCleanupService = dataCleanupService;
        this.appConfig = appConfig;
    }

    /**
     * 取得系統健康狀態
     *
     * @return 系統健康狀態
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<SystemHealthDto>> getHealth() {
        // 降低日誌級別以減少干擾
        logger.trace("執行系統健康檢查");

        try {
            Map<String, ComponentHealth> components = new HashMap<>();
            HealthStatus overallStatus = HealthStatus.UP;

            // 檢查資料檔案
            ComponentHealth dataFileHealth = checkDataFile();
            components.put("dataFile", dataFileHealth);
            if (dataFileHealth.status() != HealthStatus.UP) {
                overallStatus = HealthStatus.DEGRADED;
            }

            // 檢查資料存取
            ComponentHealth dataAccessHealth = checkDataAccess();
            components.put("dataAccess", dataAccessHealth);
            if (dataAccessHealth.status() != HealthStatus.UP) {
                overallStatus = HealthStatus.DEGRADED;
            }

            // 取得統計資訊
            Long dataFileSize = getDataFileSize();
            Integer totalRecords = getTotalRecords();
            LocalDateTime lastFetchAt = getLastFetchTime();

            SystemHealthDto healthDto = new SystemHealthDto(
                    overallStatus,
                    components,
                    lastFetchAt,
                    dataFileSize,
                    totalRecords,
                    LocalDateTime.now());

            logger.info("健康檢查完成: 狀態={}, 記錄數={}", overallStatus, totalRecords);
            return ResponseEntity.ok(ApiResponse.success(healthDto));

        } catch (Exception e) {
            logger.error("健康檢查失敗: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("健康檢查失敗: " + e.getMessage()));
        }
    }

    /**
     * 取得系統資訊
     *
     * @return 系統資訊
     */
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<SystemInfo>> getSystemInfo() {
        logger.debug("取得系統資訊");

        DataCleanupService.CleanupStatistics stats = dataCleanupService.getCleanupStatistics();
        Path dataFilePath = getDataFilePath();

        SystemInfo info = new SystemInfo(
                "ETF 00981A 持倉追蹤器",
                "1.0.0",
                dataFilePath.toString(),
                getDataFileSize(),
                stats.totalRecords(),
                stats.oldestDate(),
                stats.newestDate(),
                stats.getDataSpanDays(),
                appConfig.getData().getRetentionDays(),
                LocalDateTime.now());

        return ResponseEntity.ok(ApiResponse.success(info));
    }

    /**
     * 檢查資料檔案健康狀態
     */
    private ComponentHealth checkDataFile() {
        Path dataFilePath = getDataFilePath();

        if (!Files.exists(dataFilePath)) {
            return new ComponentHealth("資料檔案", HealthStatus.DEGRADED, "檔案不存在（尚未有資料）");
        }

        if (!Files.isReadable(dataFilePath)) {
            return ComponentHealth.down("資料檔案", "檔案無法讀取");
        }

        if (!Files.isWritable(dataFilePath)) {
            return new ComponentHealth("資料檔案", HealthStatus.DEGRADED, "檔案無法寫入");
        }

        return ComponentHealth.up("資料檔案");
    }

    /**
     * 檢查資料存取健康狀態
     */
    private ComponentHealth checkDataAccess() {
        try {
            // 嘗試讀取可用日期，驗證資料存取是否正常
            List<LocalDate> dates = storageService.getAvailableDates();
            if (dates.isEmpty()) {
                return new ComponentHealth("資料存取", HealthStatus.UP, "正常（尚無資料）");
            }
            return ComponentHealth.up("資料存取");
        } catch (Exception e) {
            return ComponentHealth.down("資料存取", "存取失敗: " + e.getMessage());
        }
    }

    /**
     * 取得資料檔案路徑
     */
    private Path getDataFilePath() {
        return Path.of(appConfig.getData().getStoragePath())
                .resolve(appConfig.getData().getFileName());
    }

    /**
     * 取得資料檔案大小
     */
    private Long getDataFileSize() {
        Path dataFilePath = getDataFilePath();
        if (!Files.exists(dataFilePath)) {
            return null;
        }
        try {
            return Files.size(dataFilePath);
        } catch (IOException e) {
            logger.warn("無法取得檔案大小: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 取得總記錄數
     */
    private Integer getTotalRecords() {
        DataCleanupService.CleanupStatistics stats = dataCleanupService.getCleanupStatistics();
        return stats.totalRecords();
    }

    /**
     * 取得最後抓取時間（使用最新資料日期作為近似值）
     */
    private LocalDateTime getLastFetchTime() {
        Optional<DailySnapshot> latest = storageService.getLatestSnapshot();
        if (latest.isPresent()) {
            // 假設資料在當天抓取
            return latest.get().getDate().atStartOfDay();
        }
        return null;
    }

    /**
     * 取得儲存服務資訊
     * <p>
     * 提供資料來源狀態，用於前端判斷是否允許編輯
     * </p>
     *
     * @return 儲存服務資訊
     */
    @GetMapping("/storage-info")
    public ResponseEntity<ApiResponse<StorageInfo>> getStorageInfo() {
        boolean readOnly = storageService.isReadOnly();
        String dataSource = storageService.getDataSourceInfo();

        StorageInfo info = new StorageInfo(
                dataSource,
                readOnly,
                readOnly ? "資料來源為 Google Sheets，僅供讀取" : "資料來源為本地 Excel，可進行編輯",
                LocalDateTime.now());

        logger.debug("儲存服務資訊: readOnly={}, dataSource={}", readOnly, dataSource);
        return ResponseEntity.ok(ApiResponse.success(info));
    }

    /**
     * 儲存服務資訊
     *
     * @param dataSource 資料來源名稱
     * @param readOnly   是否唯讀
     * @param message    說明訊息
     * @param checkedAt  檢查時間
     */
    public record StorageInfo(
            String dataSource,
            boolean readOnly,
            String message,
            LocalDateTime checkedAt) {
    }

    /**
     * 系統資訊
     *
     * @param appName       應用程式名稱
     * @param version       版本號
     * @param dataFilePath  資料檔案路徑
     * @param dataFileSize  資料檔案大小（bytes）
     * @param totalRecords  總記錄數
     * @param oldestDate    最舊資料日期
     * @param newestDate    最新資料日期
     * @param dataSpanDays  資料跨越天數
     * @param retentionDays 保留天數
     * @param checkedAt     檢查時間
     */
    public record SystemInfo(
            String appName,
            String version,
            String dataFilePath,
            Long dataFileSize,
            Integer totalRecords,
            LocalDate oldestDate,
            LocalDate newestDate,
            Integer dataSpanDays,
            Integer retentionDays,
            LocalDateTime checkedAt) {
    }
}
