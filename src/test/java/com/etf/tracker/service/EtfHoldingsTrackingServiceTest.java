package com.etf.tracker.service;

import com.etf.tracker.dto.DailyChangeSummaryDto;
import com.etf.tracker.dto.HoldingDto;
import com.etf.tracker.model.DailyChange;
import com.etf.tracker.model.EtfHolding;
import com.etf.tracker.repository.DailyChangeRepository;
import com.etf.tracker.repository.EtfHoldingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ETF Holdings Tracking Service Tests")
class EtfHoldingsTrackingServiceTest {

    @Mock
    private EtfHoldingRepository holdingRepository;

    @Mock
    private DailyChangeRepository changeRepository;

    @Mock
    private EtfDataCrawlerService crawlerService;

    private EtfHoldingsTrackingService trackingService;

    private static final String ETF_SYMBOL = "ARKK";
    private static final LocalDate TODAY = LocalDate.now();
    private static final LocalDate YESTERDAY = LocalDate.now().minusDays(1);

    @BeforeEach
    void setUp() {
        trackingService = new EtfHoldingsTrackingService(
            holdingRepository,
            changeRepository,
            List.of(crawlerService)
        );
    }

    @Test
    @DisplayName("當沒有前一天資料時，所有成分股都應標記為新增")
    void shouldMarkAllAsNewWhenNoPreviousData() {
        // Given
        List<HoldingDto> currentHoldings = List.of(
            createHoldingDto("TSLA", "Tesla Inc", 1000L, 10.0),
            createHoldingDto("AAPL", "Apple Inc", 500L, 5.0)
        );

        when(crawlerService.isSupported(ETF_SYMBOL)).thenReturn(true);
        when(crawlerService.crawlHoldings(ETF_SYMBOL)).thenReturn(currentHoldings);
        when(holdingRepository.findLatestRecordDate(ETF_SYMBOL)).thenReturn(Optional.empty());

        // When
        DailyChangeSummaryDto summary = trackingService.updateAndTrack(ETF_SYMBOL);

        // Then
        assertEquals(ETF_SYMBOL, summary.getEtfSymbol());
        assertEquals(2, summary.getNewHoldings());
        assertEquals(0, summary.getRemovedHoldings());
        assertEquals(0, summary.getIncreasedHoldings());
        assertEquals(0, summary.getDecreasedHoldings());
        assertEquals(2, summary.getTotalChanges());

        verify(holdingRepository).saveAll(any());
        verify(changeRepository).saveAll(any());
    }

    @Test
    @DisplayName("當成分股被移除時，應標記為 REMOVED")
    void shouldDetectRemovedHoldings() {
        // Given
        List<EtfHolding> previousHoldings = List.of(
            createEtfHolding("TSLA", "Tesla Inc", 1000L, 10.0),
            createEtfHolding("AAPL", "Apple Inc", 500L, 5.0),
            createEtfHolding("GOOG", "Google", 300L, 3.0)  // 將被移除
        );

        List<HoldingDto> currentHoldings = List.of(
            createHoldingDto("TSLA", "Tesla Inc", 1000L, 10.0),
            createHoldingDto("AAPL", "Apple Inc", 500L, 5.0)
        );

        when(crawlerService.isSupported(ETF_SYMBOL)).thenReturn(true);
        when(crawlerService.crawlHoldings(ETF_SYMBOL)).thenReturn(currentHoldings);
        when(holdingRepository.findLatestRecordDate(ETF_SYMBOL)).thenReturn(Optional.of(YESTERDAY));
        when(holdingRepository.findByEtfSymbolAndRecordDate(ETF_SYMBOL, YESTERDAY)).thenReturn(previousHoldings);

        // When
        DailyChangeSummaryDto summary = trackingService.updateAndTrack(ETF_SYMBOL);

        // Then
        assertEquals(1, summary.getRemovedHoldings());
        assertTrue(summary.getChanges().stream()
            .anyMatch(c -> c.getStockSymbol().equals("GOOG") && 
                          c.getChangeType() == DailyChange.ChangeType.REMOVED));
    }

    @Test
    @DisplayName("當持股數量增加時，應標記為 INCREASED")
    void shouldDetectIncreasedHoldings() {
        // Given
        List<EtfHolding> previousHoldings = List.of(
            createEtfHolding("TSLA", "Tesla Inc", 1000L, 10.0)
        );

        List<HoldingDto> currentHoldings = List.of(
            createHoldingDto("TSLA", "Tesla Inc", 1500L, 15.0)  // 增持
        );

        when(crawlerService.isSupported(ETF_SYMBOL)).thenReturn(true);
        when(crawlerService.crawlHoldings(ETF_SYMBOL)).thenReturn(currentHoldings);
        when(holdingRepository.findLatestRecordDate(ETF_SYMBOL)).thenReturn(Optional.of(YESTERDAY));
        when(holdingRepository.findByEtfSymbolAndRecordDate(ETF_SYMBOL, YESTERDAY)).thenReturn(previousHoldings);

        // When
        DailyChangeSummaryDto summary = trackingService.updateAndTrack(ETF_SYMBOL);

        // Then
        assertEquals(1, summary.getIncreasedHoldings());
        assertEquals(0, summary.getDecreasedHoldings());

        var change = summary.getChanges().get(0);
        assertEquals(DailyChange.ChangeType.INCREASED, change.getChangeType());
        assertEquals(500L, change.getSharesChange());
    }

    @Test
    @DisplayName("當持股數量減少時，應標記為 DECREASED")
    void shouldDetectDecreasedHoldings() {
        // Given
        List<EtfHolding> previousHoldings = List.of(
            createEtfHolding("TSLA", "Tesla Inc", 1000L, 10.0)
        );

        List<HoldingDto> currentHoldings = List.of(
            createHoldingDto("TSLA", "Tesla Inc", 800L, 8.0)  // 減持
        );

        when(crawlerService.isSupported(ETF_SYMBOL)).thenReturn(true);
        when(crawlerService.crawlHoldings(ETF_SYMBOL)).thenReturn(currentHoldings);
        when(holdingRepository.findLatestRecordDate(ETF_SYMBOL)).thenReturn(Optional.of(YESTERDAY));
        when(holdingRepository.findByEtfSymbolAndRecordDate(ETF_SYMBOL, YESTERDAY)).thenReturn(previousHoldings);

        // When
        DailyChangeSummaryDto summary = trackingService.updateAndTrack(ETF_SYMBOL);

        // Then
        assertEquals(0, summary.getIncreasedHoldings());
        assertEquals(1, summary.getDecreasedHoldings());

        var change = summary.getChanges().get(0);
        assertEquals(DailyChange.ChangeType.DECREASED, change.getChangeType());
        assertEquals(-200L, change.getSharesChange());
    }

    @Test
    @DisplayName("當爬取資料為空時，應返回空的摘要")
    void shouldReturnEmptySummaryWhenNoCrawledData() {
        // Given
        when(crawlerService.isSupported(ETF_SYMBOL)).thenReturn(true);
        when(crawlerService.crawlHoldings(ETF_SYMBOL)).thenReturn(Collections.emptyList());

        // When
        DailyChangeSummaryDto summary = trackingService.updateAndTrack(ETF_SYMBOL);

        // Then
        assertEquals(0, summary.getTotalChanges());
        assertTrue(summary.getChanges().isEmpty());

        verify(holdingRepository, never()).saveAll(any());
        verify(changeRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("手動更新成分股資料")
    void shouldUpdateHoldingsManually() {
        // Given
        List<HoldingDto> holdings = List.of(
            createHoldingDto("NVDA", "NVIDIA Corp", 2000L, 8.0),
            createHoldingDto("META", "Meta Platforms", 1500L, 6.0)
        );

        when(holdingRepository.findLatestRecordDate(ETF_SYMBOL)).thenReturn(Optional.empty());

        // When
        DailyChangeSummaryDto summary = trackingService.updateHoldingsManually(ETF_SYMBOL, holdings);

        // Then
        assertEquals(2, summary.getNewHoldings());
        verify(holdingRepository).saveAll(any());
    }

    @Test
    @DisplayName("查詢當前成分股")
    void shouldGetCurrentHoldings() {
        // Given
        List<EtfHolding> holdings = List.of(
            createEtfHolding("TSLA", "Tesla Inc", 1000L, 10.0)
        );

        when(holdingRepository.findLatestRecordDate(ETF_SYMBOL)).thenReturn(Optional.of(TODAY));
        when(holdingRepository.findByEtfSymbolAndRecordDate(ETF_SYMBOL, TODAY)).thenReturn(holdings);

        // When
        List<EtfHolding> result = trackingService.getCurrentHoldings(ETF_SYMBOL);

        // Then
        assertEquals(1, result.size());
        assertEquals("TSLA", result.get(0).getStockSymbol());
    }

    // Helper methods
    private HoldingDto createHoldingDto(String symbol, String name, Long shares, Double weight) {
        return HoldingDto.builder()
            .stockSymbol(symbol)
            .stockName(name)
            .shares(shares)
            .weight(weight)
            .marketValue(shares * 100.0)
            .build();
    }

    private EtfHolding createEtfHolding(String symbol, String name, Long shares, Double weight) {
        return EtfHolding.builder()
            .etfSymbol(ETF_SYMBOL)
            .recordDate(YESTERDAY)
            .stockSymbol(symbol)
            .stockName(name)
            .shares(shares)
            .weight(weight)
            .marketValue(shares * 100.0)
            .build();
    }
}
