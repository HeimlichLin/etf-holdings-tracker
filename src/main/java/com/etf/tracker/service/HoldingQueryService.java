package com.etf.tracker.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.etf.tracker.model.DailySnapshot;
import com.etf.tracker.model.Holding;
import com.etf.tracker.service.HoldingQueryService.HoldingStatistics;

/**
 * 持倉查詢服務
 * <p>
 * 提供 ETF 持倉資料的查詢、搜尋、排序與統計功能
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
@Service
public class HoldingQueryService {

    private static final Logger logger = LoggerFactory.getLogger(HoldingQueryService.class);

    private final ExcelStorageService excelStorageService;

    public HoldingQueryService(ExcelStorageService excelStorageService) {
        this.excelStorageService = excelStorageService;
    }

    /**
     * 取得指定日期的快照
     *
     * @param date 日期
     * @return 快照，如果不存在則為空
     */
    public Optional<DailySnapshot> getSnapshotByDate(LocalDate date) {
        if (date == null) {
            logger.warn("查詢日期為 null");
            return Optional.empty();
        }
        logger.debug("查詢日期 {} 的持倉資料", date);
        return excelStorageService.getSnapshot(date);
    }

    /**
     * 取得最新的快照
     *
     * @return 最新快照，如果沒有資料則為空
     */
    public Optional<DailySnapshot> getLatestSnapshot() {
        logger.debug("查詢最新持倉資料");
        return excelStorageService.getLatestSnapshot();
    }

    /**
     * 取得所有可用日期（降序排列）
     *
     * @return 日期清單
     */
    public List<LocalDate> getAvailableDates() {
        logger.debug("查詢可用日期");
        return excelStorageService.getAvailableDates();
    }

    /**
     * 依股票代號搜尋持倉
     *
     * @param stockCode 股票代號（部分符合）
     * @return 符合條件的持倉清單
     */
    public List<Holding> searchByStockCode(String stockCode) {
        if (stockCode == null || stockCode.isBlank()) {
            return getAllHoldings();
        }

        logger.debug("依股票代號搜尋: {}", stockCode);
        return getLatestSnapshot()
                .map(snapshot -> snapshot.getHoldings().stream()
                        .filter(h -> h.getStockCode().contains(stockCode))
                        .toList())
                .orElse(Collections.emptyList());
    }

    /**
     * 依股票名稱搜尋持倉
     *
     * @param stockName 股票名稱（部分符合）
     * @return 符合條件的持倉清單
     */
    public List<Holding> searchByStockName(String stockName) {
        if (stockName == null || stockName.isBlank()) {
            return getAllHoldings();
        }

        logger.debug("依股票名稱搜尋: {}", stockName);
        return getLatestSnapshot()
                .map(snapshot -> snapshot.getHoldings().stream()
                        .filter(h -> h.getStockName().toLowerCase().contains(stockName.toLowerCase()))
                        .toList())
                .orElse(Collections.emptyList());
    }

    /**
     * 通用搜尋（股票代號或名稱）
     *
     * @param keyword 關鍵字
     * @return 符合條件的持倉清單
     */
    public List<Holding> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllHoldings();
        }

        String lowerKeyword = keyword.toLowerCase();
        logger.debug("通用搜尋: {}", keyword);

        return getLatestSnapshot()
                .map(snapshot -> snapshot.getHoldings().stream()
                        .filter(h -> h.getStockCode().toLowerCase().contains(lowerKeyword) ||
                                h.getStockName().toLowerCase().contains(lowerKeyword))
                        .toList())
                .orElse(Collections.emptyList());
    }

    /**
     * 取得持倉統計資訊
     *
     * @return 統計資訊
     */
    public HoldingStatistics getStatistics() {
        return getLatestSnapshot()
                .map(snapshot -> new HoldingStatistics(
                        snapshot.getDate(),
                        snapshot.getTotalCount(),
                        snapshot.getTotalWeight()))
                .orElse(HoldingStatistics.empty());
    }

    /**
     * 取得分頁後的持倉清單
     *
     * @param page     頁碼（從 0 開始）
     * @param pageSize 每頁筆數
     * @return 分頁後的持倉清單
     */
    public List<Holding> getHoldingsPage(int page, int pageSize) {
        if (page < 0 || pageSize <= 0) {
            return Collections.emptyList();
        }

        List<Holding> all = getAllHoldings();
        int start = page * pageSize;

        if (start >= all.size()) {
            return Collections.emptyList();
        }

        int end = Math.min(start + pageSize, all.size());
        return all.subList(start, end);
    }

    /**
     * 取得依權重排序的持倉清單
     *
     * @param ascending true 為升序，false 為降序
     * @return 排序後的持倉清單
     */
    public List<Holding> getHoldingsSortedByWeight(boolean ascending) {
        List<Holding> holdings = new ArrayList<>(getAllHoldings());

        Comparator<Holding> comparator = Comparator.comparing(Holding::getWeight);
        if (!ascending) {
            comparator = comparator.reversed();
        }

        holdings.sort(comparator);
        return holdings;
    }

    /**
     * 取得依股票代號排序的持倉清單
     *
     * @param ascending true 為升序，false 為降序
     * @return 排序後的持倉清單
     */
    public List<Holding> getHoldingsSortedByCode(boolean ascending) {
        List<Holding> holdings = new ArrayList<>(getAllHoldings());

        Comparator<Holding> comparator = Comparator.comparing(Holding::getStockCode);
        if (!ascending) {
            comparator = comparator.reversed();
        }

        holdings.sort(comparator);
        return holdings;
    }

    /**
     * 取得所有持倉
     *
     * @return 持倉清單
     */
    private List<Holding> getAllHoldings() {
        return getLatestSnapshot()
                .map(DailySnapshot::getHoldings)
                .orElse(Collections.emptyList());
    }

    /**
     * 持倉統計資訊
     */
    public record HoldingStatistics(
            LocalDate date,
            int totalCount,
            BigDecimal totalWeight) {

        /**
         * 建立空的統計資訊
         */
        public static HoldingStatistics empty() {
            return new HoldingStatistics(null, 0, BigDecimal.ZERO);
        }
    }
}
