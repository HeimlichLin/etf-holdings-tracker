package com.etf.tracker.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.etf.tracker.dto.HoldingChangeDto;
import com.etf.tracker.dto.RangeCompareResultDto;
import com.etf.tracker.exception.ValidationException;
import com.etf.tracker.model.ChangeType;
import com.etf.tracker.model.DailySnapshot;
import com.etf.tracker.model.Holding;

/**
 * 持倉比較服務
 * <p>
 * 提供兩個日期間持倉變化的比較功能，包括：
 * <ul>
 * <li>新進增持識別 (NEW_ADDITION)</li>
 * <li>剔除減持識別 (REMOVED)</li>
 * <li>增持計算 (INCREASED)</li>
 * <li>減持計算 (DECREASED)</li>
 * <li>不變識別 (UNCHANGED)</li>
 * </ul>
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
@Service
public class HoldingCompareService {

    private static final Logger logger = LoggerFactory.getLogger(HoldingCompareService.class);

    /** 變化比例小數位數 */
    private static final int RATIO_SCALE = 2;

    /** 權重變化小數位數 */
    private static final int WEIGHT_SCALE = 4;

    private final ExcelStorageService excelStorageService;

    public HoldingCompareService(ExcelStorageService excelStorageService) {
        this.excelStorageService = excelStorageService;
    }

    /**
     * 比較兩個日期間的持倉變化
     *
     * @param startDate 起始日期
     * @param endDate   結束日期
     * @return 區間比較結果，包含各類別的變化清單
     * @throws ValidationException 當日期無效或無資料時
     */
    public RangeCompareResultDto compareHoldings(LocalDate startDate, LocalDate endDate) {
        logger.info("開始比較持倉: {} -> {}", startDate, endDate);

        // 1. 驗證日期
        validateDates(startDate, endDate);

        // 2. 取得快照資料
        DailySnapshot startSnapshot = getSnapshotOrThrow(startDate, "起始日期無資料");
        DailySnapshot endSnapshot = getSnapshotOrThrow(endDate, "結束日期無資料");

        // 3. 建立股票代號 -> 持倉的映射
        Map<String, Holding> startHoldingsMap = buildHoldingsMap(startSnapshot.getHoldings());
        Map<String, Holding> endHoldingsMap = buildHoldingsMap(endSnapshot.getHoldings());

        // 4. 分類變化
        List<HoldingChangeDto> newAdditions = new ArrayList<>();
        List<HoldingChangeDto> removals = new ArrayList<>();
        List<HoldingChangeDto> increased = new ArrayList<>();
        List<HoldingChangeDto> decreased = new ArrayList<>();
        List<HoldingChangeDto> unchanged = new ArrayList<>();

        // 5. 處理起始日的股票
        for (Holding startHolding : startSnapshot.getHoldings()) {
            String stockCode = startHolding.getStockCode();
            Holding endHolding = endHoldingsMap.get(stockCode);

            if (endHolding == null) {
                // 剔除: 起始日存在，結束日不存在
                removals.add(createRemovalChange(startHolding));
            } else {
                // 比較股數變化
                HoldingChangeDto change = compareExistingHolding(startHolding, endHolding);
                classifyChange(change, increased, decreased, unchanged);
            }
        }

        // 6. 處理結束日的新進股票
        for (Holding endHolding : endSnapshot.getHoldings()) {
            String stockCode = endHolding.getStockCode();
            if (!startHoldingsMap.containsKey(stockCode)) {
                // 新進: 起始日不存在，結束日存在
                newAdditions.add(createNewAdditionChange(endHolding));
            }
        }

        logger.info("比較完成: 新進={}, 剔除={}, 增持={}, 減持={}, 不變={}",
                newAdditions.size(), removals.size(), increased.size(),
                decreased.size(), unchanged.size());

        return new RangeCompareResultDto(
                startDate,
                endDate,
                newAdditions,
                removals,
                increased,
                decreased,
                unchanged);
    }

    // ========== Private Methods ==========

    /**
     * 驗證日期有效性
     */
    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new ValidationException("起始日期必須早於或等於結束日期");
        }
    }

    /**
     * 取得快照，若不存在則拋出例外
     */
    private DailySnapshot getSnapshotOrThrow(LocalDate date, String errorMessage) {
        Optional<DailySnapshot> snapshot = excelStorageService.getSnapshot(date);
        if (snapshot.isEmpty()) {
            throw new ValidationException(errorMessage);
        }
        return snapshot.get();
    }

    /**
     * 建立股票代號 -> 持倉的映射
     */
    private Map<String, Holding> buildHoldingsMap(List<Holding> holdings) {
        Map<String, Holding> map = new HashMap<>();
        if (holdings != null) {
            for (Holding holding : holdings) {
                map.put(holding.getStockCode(), holding);
            }
        }
        return map;
    }

    /**
     * 建立新進變化記錄
     */
    private HoldingChangeDto createNewAdditionChange(Holding endHolding) {
        return new HoldingChangeDto(
                endHolding.getStockCode(),
                endHolding.getStockName(),
                ChangeType.NEW_ADDITION,
                null, // startShares
                endHolding.getShares(),
                endHolding.getShares(), // sharesDiff = endShares
                null, // changeRatio (新進無法計算比例)
                null, // startWeight
                endHolding.getWeight(),
                endHolding.getWeight() // weightDiff = endWeight
        );
    }

    /**
     * 建立剔除變化記錄
     */
    private HoldingChangeDto createRemovalChange(Holding startHolding) {
        BigDecimal changeRatio = new BigDecimal("-100.00")
                .setScale(RATIO_SCALE, RoundingMode.HALF_UP);

        return new HoldingChangeDto(
                startHolding.getStockCode(),
                startHolding.getStockName(),
                ChangeType.REMOVED,
                startHolding.getShares(),
                null, // endShares
                -startHolding.getShares(), // sharesDiff 為負
                changeRatio, // -100%
                startHolding.getWeight(),
                null, // endWeight
                startHolding.getWeight().negate() // weightDiff 為負
        );
    }

    /**
     * 比較現有持倉的變化
     */
    private HoldingChangeDto compareExistingHolding(Holding startHolding, Holding endHolding) {
        long sharesDiff = endHolding.getShares() - startHolding.getShares();
        BigDecimal changeRatio = calculateChangeRatio(startHolding.getShares(), sharesDiff);
        BigDecimal weightDiff = calculateWeightDiff(startHolding.getWeight(), endHolding.getWeight());
        ChangeType changeType = determineChangeType(sharesDiff);

        return new HoldingChangeDto(
                endHolding.getStockCode(),
                endHolding.getStockName(),
                changeType,
                startHolding.getShares(),
                endHolding.getShares(),
                sharesDiff,
                changeRatio,
                startHolding.getWeight(),
                endHolding.getWeight(),
                weightDiff);
    }

    /**
     * 計算變化比例
     */
    private BigDecimal calculateChangeRatio(Long startShares, long sharesDiff) {
        if (startShares == null || startShares == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(sharesDiff)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(startShares), RATIO_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 計算權重變化
     */
    private BigDecimal calculateWeightDiff(BigDecimal startWeight, BigDecimal endWeight) {
        if (startWeight == null) {
            return endWeight;
        }
        if (endWeight == null) {
            return startWeight.negate();
        }
        return endWeight.subtract(startWeight).setScale(WEIGHT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 判斷變化類型
     */
    private ChangeType determineChangeType(long sharesDiff) {
        if (sharesDiff > 0) {
            return ChangeType.INCREASED;
        } else if (sharesDiff < 0) {
            return ChangeType.DECREASED;
        } else {
            return ChangeType.UNCHANGED;
        }
    }

    /**
     * 根據變化類型分類
     */
    private void classifyChange(HoldingChangeDto change,
            List<HoldingChangeDto> increased,
            List<HoldingChangeDto> decreased,
            List<HoldingChangeDto> unchanged) {
        switch (change.changeType()) {
            case INCREASED -> increased.add(change);
            case DECREASED -> decreased.add(change);
            case UNCHANGED -> unchanged.add(change);
            default -> {
                // 不應該發生
            }
        }
    }
}
