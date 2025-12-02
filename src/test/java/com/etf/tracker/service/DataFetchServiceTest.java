package com.etf.tracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.etf.tracker.config.AppConfig;
import com.etf.tracker.exception.DataFetchException;
import com.etf.tracker.model.DailySnapshot;
import com.etf.tracker.model.Holding;
import com.etf.tracker.scraper.EzMoneyScraperStrategy;
import com.etf.tracker.scraper.PlaywrightWebClient;

/**
 * DataFetchService 單元測試
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class DataFetchServiceTest {

    @Mock
    private PlaywrightWebClient webClient;

    @Mock
    private EzMoneyScraperStrategy scraperStrategy;

    @Mock
    private AppConfig appConfig;

    @Mock
    private AppConfig.ScraperConfig scraperConfig;

    private DataFetchService dataFetchService;

    @BeforeEach
    void setUp() {
        when(appConfig.getScraper()).thenReturn(scraperConfig);
        when(scraperConfig.getTargetUrl()).thenReturn("https://test.com/etf");

        dataFetchService = new DataFetchService(webClient, scraperStrategy, appConfig);
    }

    @Test
    @DisplayName("成功抓取並解析持倉資料")
    void fetchLatestHoldings_Success() {
        // Given
        String mockHtml = "<html>mock</html>";
        DailySnapshot expectedSnapshot = createMockSnapshot();

        when(webClient.fetchHtml(anyString())).thenReturn(mockHtml);
        when(scraperStrategy.parseHoldings(mockHtml)).thenReturn(expectedSnapshot);

        // When
        DailySnapshot result = dataFetchService.fetchLatestHoldings();

        // Then
        assertNotNull(result);
        assertEquals(expectedSnapshot.getDate(), result.getDate());
        assertEquals(expectedSnapshot.getTotalCount(), result.getTotalCount());

        verify(webClient).fetchHtml(anyString());
        verify(scraperStrategy).parseHoldings(mockHtml);
    }

    @Test
    @DisplayName("網路錯誤應拋出 DataFetchException")
    void fetchLatestHoldings_NetworkError_ThrowsException() {
        // Given
        when(webClient.fetchHtml(anyString()))
                .thenThrow(new DataFetchException("網路連線失敗"));

        // When & Then
        assertThrows(DataFetchException.class, () -> dataFetchService.fetchLatestHoldings());
    }

    @Test
    @DisplayName("解析錯誤應拋出 DataFetchException")
    void fetchLatestHoldings_ParseError_ThrowsException() {
        // Given
        when(webClient.fetchHtml(anyString())).thenReturn("<html></html>");
        when(scraperStrategy.parseHoldings(anyString()))
                .thenThrow(new DataFetchException("解析失敗"));

        // When & Then
        assertThrows(DataFetchException.class, () -> dataFetchService.fetchLatestHoldings());
    }

    @Test
    @DisplayName("使用正確的目標 URL")
    void fetchLatestHoldings_UsesCorrectUrl() {
        // Given
        String targetUrl = "https://test.com/etf";
        when(scraperConfig.getTargetUrl()).thenReturn(targetUrl);
        when(webClient.fetchHtml(targetUrl)).thenReturn("<html></html>");
        when(scraperStrategy.parseHoldings(anyString())).thenReturn(createMockSnapshot());

        // When
        dataFetchService.fetchLatestHoldings();

        // Then
        verify(webClient).fetchHtml(targetUrl);
    }

    @Test
    @DisplayName("回傳的快照包含所有必要欄位")
    void fetchLatestHoldings_ReturnsCompleteSnapshot() {
        // Given
        DailySnapshot mockSnapshot = createMockSnapshot();
        when(webClient.fetchHtml(anyString())).thenReturn("<html></html>");
        when(scraperStrategy.parseHoldings(anyString())).thenReturn(mockSnapshot);

        // When
        DailySnapshot result = dataFetchService.fetchLatestHoldings();

        // Then
        assertNotNull(result.getDate());
        assertNotNull(result.getHoldings());
        assertFalse(result.getHoldings().isEmpty());
        assertTrue(result.getTotalCount() > 0);
        assertNotNull(result.getTotalWeight());
    }

    private DailySnapshot createMockSnapshot() {
        Holding holding = Holding.builder()
                .stockCode("2330")
                .stockName("台積電")
                .shares(1234567L)
                .weight(new BigDecimal("12.3456"))
                .build();

        return DailySnapshot.builder()
                .date(LocalDate.now())
                .holdings(List.of(holding))
                .totalCount(1)
                .totalWeight(new BigDecimal("12.3456"))
                .build();
    }
}
