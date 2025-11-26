package com.etf.tracker.service;

import com.etf.tracker.dto.HoldingDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("ARK ETF Crawler Service Tests")
class ArkEtfCrawlerServiceTest {

    @Autowired
    private ArkEtfCrawlerService crawlerService;

    @Test
    @DisplayName("應支援 ARK 系列 ETF")
    void shouldSupportArkEtfs() {
        assertTrue(crawlerService.isSupported("ARKK"));
        assertTrue(crawlerService.isSupported("ARKW"));
        assertTrue(crawlerService.isSupported("ARKQ"));
        assertTrue(crawlerService.isSupported("ARKG"));
        assertTrue(crawlerService.isSupported("ARKF"));
        assertTrue(crawlerService.isSupported("ARKX"));
        assertTrue(crawlerService.isSupported("arkk"));  // 大小寫不敏感
    }

    @Test
    @DisplayName("不應支援非 ARK ETF")
    void shouldNotSupportNonArkEtfs() {
        assertFalse(crawlerService.isSupported("SPY"));
        assertFalse(crawlerService.isSupported("QQQ"));
        assertFalse(crawlerService.isSupported("VOO"));
        assertFalse(crawlerService.isSupported(null));
    }

    @Test
    @DisplayName("解析 CSV 內容應正確")
    void shouldParseCsvContent() {
        String csvContent = """
            date,fund,company,ticker,cusip,shares,market value ($),weight (%)
            12/01/2023,ARKK,TESLA INC,TSLA,88160R101,1234567,98765432.10,10.5
            12/01/2023,ARKK,COINBASE GLOBAL INC,COIN,19260Q107,234567,12345678.90,5.25
            """;

        List<HoldingDto> holdings = crawlerService.parseCsvContent(csvContent);

        assertEquals(2, holdings.size());

        HoldingDto tesla = holdings.get(0);
        assertEquals("TSLA", tesla.getStockSymbol());
        assertEquals("TESLA INC", tesla.getStockName());
        assertEquals(1234567L, tesla.getShares());
    }

    @Test
    @DisplayName("爬取不支援的 ETF 應返回空列表")
    void shouldReturnEmptyListForUnsupportedEtf() {
        List<HoldingDto> holdings = crawlerService.crawlHoldings("SPY");
        assertTrue(holdings.isEmpty());
    }
}
