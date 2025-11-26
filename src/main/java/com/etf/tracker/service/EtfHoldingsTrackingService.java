package com.etf.tracker.service;

import com.etf.tracker.dto.DailyChangeSummaryDto;
import com.etf.tracker.dto.HoldingDto;
import com.etf.tracker.model.DailyChange;
import com.etf.tracker.model.EtfHolding;
import com.etf.tracker.repository.DailyChangeRepository;
import com.etf.tracker.repository.EtfHoldingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ETF 成分股追蹤服務
 * 負責比較每日變動並記錄變化
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EtfHoldingsTrackingService {

    private final EtfHoldingRepository holdingRepository;
    private final DailyChangeRepository changeRepository;
    private final List<EtfDataCrawlerService> crawlerServices;

    @Value("${etf.data.retention-days:90}")
    private int retentionDays;

    /**
     * 更新 ETF 成分股資料並記錄變動
     */
    @Transactional
    public DailyChangeSummaryDto updateAndTrack(String etfSymbol) {
        LocalDate today = LocalDate.now();
        log.info("開始更新 {} 在 {} 的成分股資料", etfSymbol, today);

        // 爬取最新資料
        List<HoldingDto> latestHoldings = crawlLatestHoldings(etfSymbol);
        if (latestHoldings.isEmpty()) {
            log.warn("無法獲取 {} 的最新成分股資料", etfSymbol);
            return DailyChangeSummaryDto.builder()
                .etfSymbol(etfSymbol)
                .date(today)
                .totalChanges(0)
                .changes(Collections.emptyList())
                .build();
        }

        // 獲取前一天的資料
        Optional<LocalDate> previousDateOpt = holdingRepository.findLatestRecordDate(etfSymbol);
        List<EtfHolding> previousHoldings = previousDateOpt
            .map(date -> holdingRepository.findByEtfSymbolAndRecordDate(etfSymbol, date))
            .orElse(Collections.emptyList());

        // 比較變動
        List<DailyChange> changes = compareHoldings(etfSymbol, today, previousHoldings, latestHoldings);

        // 儲存最新持倉
        saveHoldings(etfSymbol, today, latestHoldings);

        // 儲存變動記錄
        if (!changes.isEmpty()) {
            changeRepository.saveAll(changes);
            log.info("{} 在 {} 共有 {} 項變動", etfSymbol, today, changes.size());
        }

        // 清理過期資料
        cleanupOldData();

        return buildSummary(etfSymbol, today, changes);
    }

    /**
     * 從支援的爬蟲服務獲取資料
     */
    private List<HoldingDto> crawlLatestHoldings(String etfSymbol) {
        for (EtfDataCrawlerService crawler : crawlerServices) {
            if (crawler.isSupported(etfSymbol)) {
                List<HoldingDto> holdings = crawler.crawlHoldings(etfSymbol);
                if (!holdings.isEmpty()) {
                    return holdings;
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     * 比較新舊持倉並產生變動記錄
     */
    private List<DailyChange> compareHoldings(
            String etfSymbol,
            LocalDate date,
            List<EtfHolding> previousHoldings,
            List<HoldingDto> currentHoldings) {

        List<DailyChange> changes = new ArrayList<>();

        // 建立對照表
        Map<String, EtfHolding> previousMap = previousHoldings.stream()
            .collect(Collectors.toMap(EtfHolding::getStockSymbol, Function.identity()));

        Map<String, HoldingDto> currentMap = currentHoldings.stream()
            .collect(Collectors.toMap(HoldingDto::getStockSymbol, Function.identity()));

        // 檢查新增和變動的成分股
        for (HoldingDto current : currentHoldings) {
            EtfHolding previous = previousMap.get(current.getStockSymbol());

            if (previous == null) {
                // 新增成分股
                changes.add(buildChange(etfSymbol, date, current, null, DailyChange.ChangeType.NEW));
            } else {
                // 檢查持股變動
                long sharesDiff = current.getShares() - previous.getShares();
                if (sharesDiff != 0) {
                    DailyChange.ChangeType type = sharesDiff > 0 
                        ? DailyChange.ChangeType.INCREASED 
                        : DailyChange.ChangeType.DECREASED;
                    changes.add(buildChange(etfSymbol, date, current, previous, type));
                }
            }
        }

        // 檢查被移除的成分股
        for (EtfHolding previous : previousHoldings) {
            if (!currentMap.containsKey(previous.getStockSymbol())) {
                changes.add(buildRemovedChange(etfSymbol, date, previous));
            }
        }

        return changes;
    }

    private DailyChange buildChange(
            String etfSymbol,
            LocalDate date,
            HoldingDto current,
            EtfHolding previous,
            DailyChange.ChangeType type) {

        Long prevShares = previous != null ? previous.getShares() : 0L;
        Double prevWeight = previous != null ? previous.getWeight() : 0.0;

        return DailyChange.builder()
            .etfSymbol(etfSymbol)
            .changeDate(date)
            .stockSymbol(current.getStockSymbol())
            .stockName(current.getStockName())
            .changeType(type)
            .previousShares(prevShares)
            .currentShares(current.getShares())
            .sharesChange(current.getShares() - prevShares)
            .previousWeight(prevWeight)
            .currentWeight(current.getWeight())
            .weightChange(current.getWeight() - prevWeight)
            .build();
    }

    private DailyChange buildRemovedChange(String etfSymbol, LocalDate date, EtfHolding previous) {
        return DailyChange.builder()
            .etfSymbol(etfSymbol)
            .changeDate(date)
            .stockSymbol(previous.getStockSymbol())
            .stockName(previous.getStockName())
            .changeType(DailyChange.ChangeType.REMOVED)
            .previousShares(previous.getShares())
            .currentShares(0L)
            .sharesChange(-previous.getShares())
            .previousWeight(previous.getWeight())
            .currentWeight(0.0)
            .weightChange(-previous.getWeight())
            .build();
    }

    /**
     * 儲存成分股資料
     */
    private void saveHoldings(String etfSymbol, LocalDate date, List<HoldingDto> holdings) {
        List<EtfHolding> entities = holdings.stream()
            .map(h -> EtfHolding.builder()
                .etfSymbol(etfSymbol)
                .recordDate(date)
                .stockSymbol(h.getStockSymbol())
                .stockName(h.getStockName())
                .shares(h.getShares())
                .marketValue(h.getMarketValue())
                .weight(h.getWeight())
                .build())
            .toList();

        holdingRepository.saveAll(entities);
    }

    /**
     * 清理超過保留期限的舊資料
     */
    @Transactional
    public void cleanupOldData() {
        LocalDate cutoffDate = LocalDate.now().minusDays(retentionDays);
        holdingRepository.deleteOlderThan(cutoffDate);
        changeRepository.deleteOlderThan(cutoffDate);
        log.info("已清理 {} 之前的舊資料", cutoffDate);
    }

    /**
     * 建立變動摘要
     */
    private DailyChangeSummaryDto buildSummary(String etfSymbol, LocalDate date, List<DailyChange> changes) {
        Map<DailyChange.ChangeType, Long> countByType = changes.stream()
            .collect(Collectors.groupingBy(DailyChange::getChangeType, Collectors.counting()));

        List<DailyChangeSummaryDto.DailyChangeDto> changeDtos = changes.stream()
            .map(c -> DailyChangeSummaryDto.DailyChangeDto.builder()
                .stockSymbol(c.getStockSymbol())
                .stockName(c.getStockName())
                .changeType(c.getChangeType())
                .previousShares(c.getPreviousShares())
                .currentShares(c.getCurrentShares())
                .sharesChange(c.getSharesChange())
                .previousWeight(c.getPreviousWeight())
                .currentWeight(c.getCurrentWeight())
                .weightChange(c.getWeightChange())
                .build())
            .toList();

        return DailyChangeSummaryDto.builder()
            .etfSymbol(etfSymbol)
            .date(date)
            .totalChanges(changes.size())
            .newHoldings(countByType.getOrDefault(DailyChange.ChangeType.NEW, 0L).intValue())
            .removedHoldings(countByType.getOrDefault(DailyChange.ChangeType.REMOVED, 0L).intValue())
            .increasedHoldings(countByType.getOrDefault(DailyChange.ChangeType.INCREASED, 0L).intValue())
            .decreasedHoldings(countByType.getOrDefault(DailyChange.ChangeType.DECREASED, 0L).intValue())
            .changes(changeDtos)
            .build();
    }

    /**
     * 查詢指定日期範圍的變動記錄
     */
    public List<DailyChange> getChanges(String etfSymbol, LocalDate startDate, LocalDate endDate) {
        return changeRepository.findByEtfSymbolAndChangeDateBetweenOrderByChangeDateDesc(
            etfSymbol, startDate, endDate);
    }

    /**
     * 查詢特定股票的變動歷史
     */
    public List<DailyChange> getStockHistory(String etfSymbol, String stockSymbol) {
        return changeRepository.findByEtfSymbolAndStockSymbolOrderByChangeDateDesc(etfSymbol, stockSymbol);
    }

    /**
     * 查詢當日的成分股
     */
    public List<EtfHolding> getCurrentHoldings(String etfSymbol) {
        Optional<LocalDate> latestDate = holdingRepository.findLatestRecordDate(etfSymbol);
        return latestDate
            .map(date -> holdingRepository.findByEtfSymbolAndRecordDate(etfSymbol, date))
            .orElse(Collections.emptyList());
    }

    /**
     * 手動更新成分股資料 (用於測試或手動輸入)
     */
    @Transactional
    public DailyChangeSummaryDto updateHoldingsManually(String etfSymbol, List<HoldingDto> holdings) {
        LocalDate today = LocalDate.now();
        log.info("手動更新 {} 在 {} 的成分股資料", etfSymbol, today);

        // 獲取前一天的資料
        Optional<LocalDate> previousDateOpt = holdingRepository.findLatestRecordDate(etfSymbol);
        List<EtfHolding> previousHoldings = previousDateOpt
            .filter(date -> !date.equals(today))
            .map(date -> holdingRepository.findByEtfSymbolAndRecordDate(etfSymbol, date))
            .orElse(Collections.emptyList());

        // 比較變動
        List<DailyChange> changes = compareHoldings(etfSymbol, today, previousHoldings, holdings);

        // 儲存最新持倉
        saveHoldings(etfSymbol, today, holdings);

        // 儲存變動記錄
        if (!changes.isEmpty()) {
            changeRepository.saveAll(changes);
        }

        return buildSummary(etfSymbol, today, changes);
    }
}
