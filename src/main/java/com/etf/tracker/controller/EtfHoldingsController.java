package com.etf.tracker.controller;

import com.etf.tracker.dto.DailyChangeSummaryDto;
import com.etf.tracker.dto.HoldingDto;
import com.etf.tracker.model.DailyChange;
import com.etf.tracker.model.EtfHolding;
import com.etf.tracker.service.EtfHoldingsTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * ETF 成分股追蹤 REST API
 */
@RestController
@RequestMapping("/api/etf")
@RequiredArgsConstructor
public class EtfHoldingsController {

    private final EtfHoldingsTrackingService trackingService;

    /**
     * 手動觸發更新並追蹤 ETF 成分股變動
     */
    @PostMapping("/{symbol}/track")
    public ResponseEntity<DailyChangeSummaryDto> trackEtf(@PathVariable String symbol) {
        DailyChangeSummaryDto summary = trackingService.updateAndTrack(symbol.toUpperCase());
        return ResponseEntity.ok(summary);
    }

    /**
     * 查詢當前成分股
     */
    @GetMapping("/{symbol}/holdings")
    public ResponseEntity<List<EtfHolding>> getCurrentHoldings(@PathVariable String symbol) {
        List<EtfHolding> holdings = trackingService.getCurrentHoldings(symbol.toUpperCase());
        return ResponseEntity.ok(holdings);
    }

    /**
     * 查詢指定日期範圍的變動記錄
     */
    @GetMapping("/{symbol}/changes")
    public ResponseEntity<List<DailyChange>> getChanges(
            @PathVariable String symbol,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<DailyChange> changes = trackingService.getChanges(symbol.toUpperCase(), startDate, endDate);
        return ResponseEntity.ok(changes);
    }

    /**
     * 查詢最近一季的變動記錄
     */
    @GetMapping("/{symbol}/changes/quarter")
    public ResponseEntity<List<DailyChange>> getQuarterlyChanges(@PathVariable String symbol) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(90);
        List<DailyChange> changes = trackingService.getChanges(symbol.toUpperCase(), startDate, endDate);
        return ResponseEntity.ok(changes);
    }

    /**
     * 查詢特定股票的變動歷史
     */
    @GetMapping("/{symbol}/stock/{stockSymbol}/history")
    public ResponseEntity<List<DailyChange>> getStockHistory(
            @PathVariable String symbol,
            @PathVariable String stockSymbol) {
        List<DailyChange> history = trackingService.getStockHistory(
            symbol.toUpperCase(), stockSymbol.toUpperCase());
        return ResponseEntity.ok(history);
    }

    /**
     * 手動輸入成分股資料
     */
    @PostMapping("/{symbol}/holdings")
    public ResponseEntity<DailyChangeSummaryDto> updateHoldings(
            @PathVariable String symbol,
            @RequestBody List<HoldingDto> holdings) {
        DailyChangeSummaryDto summary = trackingService.updateHoldingsManually(
            symbol.toUpperCase(), holdings);
        return ResponseEntity.ok(summary);
    }

    /**
     * 手動觸發清理過期資料
     */
    @PostMapping("/cleanup")
    public ResponseEntity<String> cleanup() {
        trackingService.cleanupOldData();
        return ResponseEntity.ok("清理完成");
    }
}
