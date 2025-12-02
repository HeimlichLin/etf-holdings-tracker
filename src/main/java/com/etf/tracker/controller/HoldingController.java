package com.etf.tracker.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.etf.tracker.dto.ApiResponse;
import com.etf.tracker.dto.AvailableDatesDto;
import com.etf.tracker.dto.CleanupResultDto;
import com.etf.tracker.dto.DailySnapshotDto;
import com.etf.tracker.dto.HoldingDto;
import com.etf.tracker.dto.RangeCompareResultDto;
import com.etf.tracker.dto.mapper.DailySnapshotMapper;
import com.etf.tracker.dto.mapper.HoldingMapper;
import com.etf.tracker.model.DailySnapshot;
import com.etf.tracker.model.Holding;
import com.etf.tracker.service.DataCleanupService;
import com.etf.tracker.service.DataFetchService;
import com.etf.tracker.service.ExcelStorageService;
import com.etf.tracker.service.HoldingCompareService;
import com.etf.tracker.service.HoldingQueryService;

/**
 * 持倉資料控制器
 * <p>
 * 提供 ETF 持倉資料的 REST API
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/holdings")
public class HoldingController {

    private static final Logger logger = LoggerFactory.getLogger(HoldingController.class);

    private final DataFetchService dataFetchService;
    private final ExcelStorageService excelStorageService;
    private final HoldingQueryService holdingQueryService;
    private final HoldingCompareService holdingCompareService;
    private final DataCleanupService dataCleanupService;

    public HoldingController(DataFetchService dataFetchService,
            ExcelStorageService excelStorageService,
            HoldingQueryService holdingQueryService,
            HoldingCompareService holdingCompareService,
            DataCleanupService dataCleanupService) {
        this.dataFetchService = dataFetchService;
        this.excelStorageService = excelStorageService;
        this.holdingQueryService = holdingQueryService;
        this.holdingCompareService = holdingCompareService;
        this.dataCleanupService = dataCleanupService;
    }

    /**
     * 抓取並儲存最新持倉資料
     *
     * @return 抓取結果
     */
    @PostMapping("/fetch")
    public ResponseEntity<ApiResponse<DailySnapshotDto>> fetchAndSaveHoldings() {
        logger.info("收到抓取持倉資料請求");

        try {
            // 1. 抓取資料
            DailySnapshot snapshot = dataFetchService.fetchLatestHoldings();

            // 2. 儲存資料
            excelStorageService.saveSnapshot(snapshot);

            // 3. 轉換並回傳
            DailySnapshotDto dto = DailySnapshotMapper.toDto(snapshot);
            ApiResponse<DailySnapshotDto> response = ApiResponse.success(dto);

            logger.info("成功抓取並儲存持倉資料: 日期={}, 成分股數量={}",
                    snapshot.getDate(), snapshot.getTotalCount());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("抓取持倉資料失敗: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("抓取持倉資料失敗: " + e.getMessage()));
        }
    }

    /**
     * 取得最新的持倉資料
     *
     * @return 最新持倉資料
     */
    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<DailySnapshotDto>> getLatestHoldings() {
        logger.info("查詢最新持倉資料");

        Optional<DailySnapshot> snapshot = excelStorageService.getLatestSnapshot();

        if (snapshot.isPresent()) {
            DailySnapshotDto dto = DailySnapshotMapper.toDto(snapshot.get());
            return ResponseEntity.ok(ApiResponse.success(dto));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 取得指定日期的持倉資料
     *
     * @param date 日期
     * @return 指定日期的持倉資料
     */
    @GetMapping("/{date}")
    public ResponseEntity<ApiResponse<DailySnapshotDto>> getHoldingsByDate(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        logger.info("查詢指定日期持倉資料: {}", date);

        Optional<DailySnapshot> snapshot = excelStorageService.getSnapshot(date);

        if (snapshot.isPresent()) {
            DailySnapshotDto dto = DailySnapshotMapper.toDto(snapshot.get());
            return ResponseEntity.ok(ApiResponse.success(dto));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 取得所有可用的日期
     *
     * @return 日期清單
     */
    @GetMapping({ "/dates", "/available-dates" })
    public ResponseEntity<ApiResponse<AvailableDatesDto>> getAvailableDates() {
        logger.info("查詢可用日期");

        List<LocalDate> dates = excelStorageService.getAvailableDates();

        LocalDate latestDate = dates.isEmpty() ? null : dates.get(0);
        LocalDate earliestDate = dates.isEmpty() ? null : dates.get(dates.size() - 1);

        AvailableDatesDto dto = new AvailableDatesDto(
                dates,
                earliestDate,
                latestDate,
                dates.size());

        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    /**
     * 清理過期資料
     *
     * @param daysToKeep 保留天數（預設 90 天）
     * @param confirm    是否確認刪除（必須為 true）
     * @return 清理結果
     */
    @DeleteMapping("/cleanup")
    public ResponseEntity<ApiResponse<CleanupResultDto>> cleanupOldData(
            @RequestParam(defaultValue = "90") int daysToKeep,
            @RequestParam(defaultValue = "false") boolean confirm) {
        logger.info("清理過期資料請求，保留 {} 天，確認: {}", daysToKeep, confirm);

        if (!confirm) {
            // 預覽模式：根據指定的保留天數計算統計
            // 保留 N 天 = 刪除 N 天前及更早的資料
            // 例如：保留 1 天 = 只保留今天，刪除昨天之前的所有資料
            LocalDate cutoffDate = LocalDate.now().minusDays(daysToKeep);

            // 計算 Excel 中符合條件的實際行數（成分股筆數）
            int totalRecords = excelStorageService.getTotalRecordCount();
            int expiredRecords = excelStorageService.countRecordsBefore(cutoffDate);
            int remainingRecords = totalRecords - expiredRecords;

            CleanupResultDto previewResult = new CleanupResultDto(
                    false,
                    expiredRecords,
                    cutoffDate,
                    remainingRecords,
                    daysToKeep,
                    java.time.LocalDateTime.now(),
                    String.format("預覽模式：將刪除 %d 筆過期資料，保留 %d 筆資料，請設定 confirm=true 確認執行",
                            expiredRecords, remainingRecords));
            return ResponseEntity.ok(ApiResponse.success(previewResult));
        }

        // 執行清理
        CleanupResultDto result = dataCleanupService.cleanupOldData(daysToKeep);

        if (result.success()) {
            logger.info("清理完成: 刪除 {} 筆資料", result.deletedRecords());
            return ResponseEntity.ok(ApiResponse.success(result));
        } else {
            logger.error("清理失敗: {}", result.message());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(result.message()));
        }
    }

    // ========== User Story 2: 查詢功能擴充 ==========

    /**
     * 搜尋持倉資料（依關鍵字）
     *
     * @param keyword 關鍵字（股票代號或名稱）
     * @return 符合條件的持倉清單
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<HoldingDto>>> searchHoldings(
            @RequestParam(required = false) String keyword) {
        logger.info("搜尋持倉資料: keyword={}", keyword);

        List<Holding> holdings = holdingQueryService.search(keyword);
        List<HoldingDto> dtoList = HoldingMapper.toDtoList(holdings);

        return ResponseEntity.ok(ApiResponse.success(dtoList));
    }

    /**
     * 取得排序後的持倉清單
     *
     * @param sortBy    排序欄位（weight, code）
     * @param ascending 是否升序
     * @return 排序後的持倉清單
     */
    @GetMapping("/sorted")
    public ResponseEntity<ApiResponse<List<HoldingDto>>> getSortedHoldings(
            @RequestParam(defaultValue = "weight") String sortBy,
            @RequestParam(defaultValue = "false") boolean ascending) {
        logger.info("取得排序持倉: sortBy={}, ascending={}", sortBy, ascending);

        List<Holding> holdings = switch (sortBy.toLowerCase()) {
            case "code" -> holdingQueryService.getHoldingsSortedByCode(ascending);
            case "weight" -> holdingQueryService.getHoldingsSortedByWeight(ascending);
            default -> holdingQueryService.getHoldingsSortedByWeight(ascending);
        };

        List<HoldingDto> dtoList = HoldingMapper.toDtoList(holdings);
        return ResponseEntity.ok(ApiResponse.success(dtoList));
    }

    /**
     * 取得分頁持倉清單
     *
     * @param page     頁碼（從 0 開始）
     * @param pageSize 每頁筆數
     * @return 分頁後的持倉清單
     */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<List<HoldingDto>>> getHoldingsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        logger.info("取得分頁持倉: page={}, pageSize={}", page, pageSize);

        List<Holding> holdings = holdingQueryService.getHoldingsPage(page, pageSize);
        List<HoldingDto> dtoList = HoldingMapper.toDtoList(holdings);

        return ResponseEntity.ok(ApiResponse.success(dtoList));
    }

    /**
     * 取得持倉統計資訊
     *
     * @return 統計資訊
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<HoldingQueryService.HoldingStatistics>> getStatistics() {
        logger.info("取得持倉統計");

        HoldingQueryService.HoldingStatistics stats = holdingQueryService.getStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // ========== User Story 3: 區間比較功能 ==========

    /**
     * 比較兩個日期間的持倉變化
     *
     * @param startDate 起始日期
     * @param endDate   結束日期
     * @return 區間比較結果，包含新進、剔除、增持、減持、不變分類
     */
    @GetMapping("/compare")
    public ResponseEntity<ApiResponse<RangeCompareResultDto>> compareHoldings(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        logger.info("比較持倉資料: {} -> {}", startDate, endDate);

        try {
            RangeCompareResultDto result = holdingCompareService.compareHoldings(startDate, endDate);

            logger.info("比較完成: 新進={}, 剔除={}, 增持={}, 減持={}, 不變={}",
                    result.newAdditionsCount(),
                    result.removalsCount(),
                    result.increasedCount(),
                    result.decreasedCount(),
                    result.unchangedCount());

            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (Exception e) {
            logger.error("比較持倉資料失敗: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
