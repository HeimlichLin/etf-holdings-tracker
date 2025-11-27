package com.etf.tracker.scraper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.etf.tracker.config.AppConfig;
import com.etf.tracker.exception.DataFetchException;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

/**
 * RetryableWebClient 單元測試
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
class RetryableWebClientTest {

    private MockWebServer mockWebServer;
    private RetryableWebClient webClient;
    private AppConfig appConfig;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // 設定測試用配置
        appConfig = createTestAppConfig();

        // 建立測試用 OkHttpClient
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();

        webClient = new RetryableWebClient(okHttpClient, appConfig);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("成功取得 HTML 內容")
    void fetchHtml_Success() {
        // Given
        String expectedHtml = "<html><body>Test Content</body></html>";
        mockWebServer.enqueue(new MockResponse()
                .setBody(expectedHtml)
                .setResponseCode(200));

        // When
        String result = webClient.fetchHtml(mockWebServer.url("/test").toString());

        // Then
        assertNotNull(result);
        assertEquals(expectedHtml, result);
    }

    @Test
    @DisplayName("HTTP 404 錯誤應拋出例外")
    void fetchHtml_NotFound_ThrowsException() {
        // Given
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        String url = mockWebServer.url("/notfound").toString();

        // When & Then
        DataFetchException exception = assertThrows(
                DataFetchException.class,
                () -> webClient.fetchHtml(url));

        assertEquals(404, exception.getHttpStatus());
        assertNotNull(exception.getTargetUrl());
    }

    @Test
    @DisplayName("HTTP 500 錯誤應重試後拋出例外")
    void fetchHtml_ServerError_RetriesAndThrowsException() {
        // Given - 設定多次 500 錯誤
        for (int i = 0; i <= appConfig.getScraper().getMaxRetries(); i++) {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        }

        String url = mockWebServer.url("/error").toString();

        // When & Then
        DataFetchException exception = assertThrows(
                DataFetchException.class,
                () -> webClient.fetchHtml(url));

        assertEquals(500, exception.getHttpStatus());
        // 應該有多次請求（原始 + 重試次數）
        assertEquals(appConfig.getScraper().getMaxRetries() + 1, mockWebServer.getRequestCount());
    }

    @Test
    @DisplayName("重試後成功取得內容")
    void fetchHtml_RetrySuccess() {
        // Given - 前兩次失敗，第三次成功
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse()
                .setBody("<html>Success</html>")
                .setResponseCode(200));

        String url = mockWebServer.url("/retry").toString();

        // When
        String result = webClient.fetchHtml(url);

        // Then
        assertNotNull(result);
        assertEquals("<html>Success</html>", result);
        assertEquals(3, mockWebServer.getRequestCount());
    }

    @Test
    @DisplayName("空回應應正確處理")
    void fetchHtml_EmptyResponse() {
        // Given
        mockWebServer.enqueue(new MockResponse()
                .setBody("")
                .setResponseCode(200));

        String url = mockWebServer.url("/empty").toString();

        // When
        String result = webClient.fetchHtml(url);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("請求應包含正確的 User-Agent")
    void fetchHtml_ContainsUserAgent() throws InterruptedException {
        // Given
        mockWebServer.enqueue(new MockResponse()
                .setBody("<html></html>")
                .setResponseCode(200));

        String url = mockWebServer.url("/agent").toString();

        // When
        webClient.fetchHtml(url);

        // Then
        var request = mockWebServer.takeRequest();
        String userAgent = request.getHeader("User-Agent");
        assertNotNull(userAgent);
        assertEquals(appConfig.getScraper().getUserAgent(), userAgent);
    }

    private AppConfig createTestAppConfig() {
        AppConfig config = new AppConfig();

        AppConfig.ScraperConfig scraperConfig = new AppConfig.ScraperConfig();
        scraperConfig.setTimeoutSeconds(5);
        scraperConfig.setMaxRetries(2);
        scraperConfig.setRetryDelays(List.of(1, 1, 1)); // 測試用短延遲
        scraperConfig.setUserAgent("TestAgent/1.0");
        config.setScraper(scraperConfig);

        return config;
    }
}
