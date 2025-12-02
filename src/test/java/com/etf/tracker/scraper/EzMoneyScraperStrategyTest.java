package com.etf.tracker.scraper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.etf.tracker.exception.DataFetchException;
import com.etf.tracker.model.DailySnapshot;
import com.etf.tracker.model.Holding;
import com.etf.tracker.test.MockHtmlData;

/**
 * EzMoneyScraperStrategy 單元測試
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
class EzMoneyScraperStrategyTest {

    private EzMoneyScraperStrategy scraper;

    @BeforeEach
    void setUp() {
        scraper = new EzMoneyScraperStrategy();
    }

    @Test
    @DisplayName("成功解析標準持倉 HTML")
    void parseHoldings_ValidHtml_ReturnsSnapshot() {
        // Given
        LocalDate testDate = LocalDate.now();
        String html = MockHtmlData.createMockHoldingsHtml(testDate);

        // When
        DailySnapshot snapshot = scraper.parseHoldings(html);

        // Then
        assertNotNull(snapshot);
        assertEquals(testDate, snapshot.getDate());
        assertEquals(5, snapshot.getTotalCount());

        List<Holding> holdings = snapshot.getHoldings();
        assertNotNull(holdings);
        assertEquals(5, holdings.size());

        // 驗證第一筆資料
        Holding first = holdings.get(0);
        assertEquals("2330", first.getStockCode());
        assertEquals("台積電", first.getStockName());
        assertEquals(1234567L, first.getShares());
        assertEquals(new BigDecimal("12.3456"), first.getWeight());
    }

    @Test
    @DisplayName("解析空表格應回傳空快照")
    void parseHoldings_EmptyTable_ReturnsEmptySnapshot() {
        // Given
        String html = MockHtmlData.createEmptyHoldingsHtml();

        // When
        DailySnapshot snapshot = scraper.parseHoldings(html);

        // Then
        assertNotNull(snapshot);
        assertEquals(0, snapshot.getTotalCount());
        assertTrue(snapshot.getHoldings().isEmpty());
    }

    @Test
    @DisplayName("解析非預期結構應拋出例外")
    void parseHoldings_MalformedHtml_ThrowsException() {
        // Given
        String html = MockHtmlData.createMalformedHtml();

        // When & Then
        assertThrows(DataFetchException.class, () -> scraper.parseHoldings(html));
    }

    @Test
    @DisplayName("解析錯誤頁面應拋出例外")
    void parseHoldings_ErrorPage_ThrowsException() {
        // Given
        String html = MockHtmlData.createErrorPageHtml();

        // When & Then
        assertThrows(DataFetchException.class, () -> scraper.parseHoldings(html));
    }

    @Test
    @DisplayName("正確解析包含逗號的股數")
    void parseHoldings_SharesWithCommas_ParsesCorrectly() {
        // Given
        String html = MockHtmlData.createMockHoldingsHtml();

        // When
        DailySnapshot snapshot = scraper.parseHoldings(html);

        // Then
        Holding holding = snapshot.getHoldings().stream()
                .filter(h -> "2330".equals(h.getStockCode()))
                .findFirst()
                .orElseThrow();

        assertEquals(1234567L, holding.getShares());
    }

    @Test
    @DisplayName("正確計算總權重")
    void parseHoldings_CalculatesTotalWeight() {
        // Given
        String html = MockHtmlData.createMockHoldingsHtml();

        // When
        DailySnapshot snapshot = scraper.parseHoldings(html);

        // Then
        assertNotNull(snapshot.getTotalWeight());
        // 12.3456 + 8.7654 + 6.5432 + 5.4321 + 4.3210 = 37.4073
        assertEquals(new BigDecimal("37.4073"), snapshot.getTotalWeight());
    }

    @Test
    @DisplayName("解析 null HTML 應拋出例外")
    void parseHoldings_NullHtml_ThrowsException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> scraper.parseHoldings(null));
    }

    @Test
    @DisplayName("解析空字串應拋出例外")
    void parseHoldings_EmptyString_ThrowsException() {
        // When & Then
        assertThrows(DataFetchException.class, () -> scraper.parseHoldings(""));
    }

    @Test
    @DisplayName("每筆持倉資料都不為 null")
    void parseHoldings_AllHoldingsNotNull() {
        // Given
        String html = MockHtmlData.createMockHoldingsHtml();

        // When
        DailySnapshot snapshot = scraper.parseHoldings(html);

        // Then
        for (Holding holding : snapshot.getHoldings()) {
            assertNotNull(holding.getStockCode());
            assertNotNull(holding.getStockName());
            assertNotNull(holding.getShares());
            assertNotNull(holding.getWeight());
        }
    }

    @Test
    @DisplayName("成功解析嵌入 JSON 的持倉資料")
    void parseHoldings_JsonEmbedded_ReturnsSnapshot() {
        // Given
        String html = """
                <html>
                <body>
                    <h5>資料日期：2023/10/27</h5>
                    <script>
                        var assetDB = [
                            {
                                "AssetCode": "ST",
                                "AssetName": "股票",
                                "Value": 33055698105,
                                "Details": [
                                    {
                                        "DetailCode": "2330",
                                        "DetailName": "台積電",
                                        "Share": 2231000,
                                        "Amount": 3212640000,
                                        "NavRate": 9.4
                                    },
                                    {
                                        "DetailCode": "3017",
                                        "DetailName": "奇鋐",
                                        "Share": 1000,
                                        "NavRate": 7
                                    }
                                ]
                            },
                            {
                                "AssetCode": "NAV",
                                "Value": 34160366355
                            }
                        ];
                    </script>
                </body>
                </html>
                """;

        // When
        DailySnapshot snapshot = scraper.parseHoldings(html);

        // Then
        assertNotNull(snapshot);
        assertEquals(LocalDate.of(2023, 10, 27), snapshot.getDate());
        assertEquals(2, snapshot.getTotalCount());

        List<Holding> holdings = snapshot.getHoldings();
        assertEquals("2330", holdings.get(0).getStockCode());
        assertEquals("台積電", holdings.get(0).getStockName());
        assertEquals(2231000L, holdings.get(0).getShares());
        assertEquals(new BigDecimal("9.4"), holdings.get(0).getWeight());
    }
}
