package com.etf.tracker.scraper;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.etf.tracker.config.AppConfig;
import com.etf.tracker.exception.DataFetchException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 可重試 HTTP 客戶端
 * <p>
 * 使用 OkHttp 實作帶有指數退避重試機制的 HTTP 客戶端
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
@Component
public class RetryableWebClient {

    private static final Logger logger = LoggerFactory.getLogger(RetryableWebClient.class);

    private final OkHttpClient httpClient;
    private final AppConfig appConfig;

    public RetryableWebClient(OkHttpClient httpClient, AppConfig appConfig) {
        this.httpClient = httpClient;
        this.appConfig = appConfig;
    }

    /**
     * 抓取指定 URL 的 HTML 內容
     * <p>
     * 如果請求失敗，會根據配置進行重試
     * </p>
     *
     * @param url 目標 URL
     * @return HTML 內容
     * @throws DataFetchException 如果所有重試都失敗
     */
    public String fetchHtml(String url) {
        AppConfig.ScraperConfig config = appConfig.getScraper();
        int maxRetries = config.getMaxRetries();

        Exception lastException = null;
        int httpStatus = 0;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                if (attempt > 0) {
                    int delaySeconds = config.getRetryDelay(attempt - 1);
                    logger.info("重試 {}/{}, 延遲 {} 秒後重新嘗試: {}", attempt, maxRetries, delaySeconds, url);
                    TimeUnit.SECONDS.sleep(delaySeconds);
                }

                Response response = executeRequest(url);
                httpStatus = response.code();

                if (response.isSuccessful()) {
                    return extractBody(response);
                }

                // 4xx 錯誤不需重試
                if (httpStatus >= 400 && httpStatus < 500) {
                    logger.error("HTTP 客戶端錯誤 {}: {}", httpStatus, url);
                    throw DataFetchException.httpError(url, httpStatus);
                }

                // 5xx 錯誤繼續重試
                logger.warn("HTTP 伺服器錯誤 {} (嘗試 {}/{}): {}", httpStatus, attempt + 1, maxRetries + 1, url);
                lastException = new IOException("HTTP " + httpStatus);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new DataFetchException("請求被中斷", e);
            } catch (DataFetchException e) {
                throw e;
            } catch (IOException e) {
                logger.warn("網路錯誤 (嘗試 {}/{}): {} - {}", attempt + 1, maxRetries + 1, url, e.getMessage());
                lastException = e;
            }
        }

        // 所有重試都失敗
        if (httpStatus >= 500) {
            throw DataFetchException.httpError(url, httpStatus);
        }

        throw new DataFetchException(
                "無法取得資料，已重試 " + maxRetries + " 次",
                lastException,
                "CONNECTION_FAILED",
                null,
                url);
    }

    /**
     * 執行 HTTP 請求
     */
    private Response executeRequest(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", appConfig.getScraper().getUserAgent())
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "zh-TW,zh;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Accept-Encoding", "gzip, deflate")
                .header("Connection", "keep-alive")
                .get()
                .build();

        return httpClient.newCall(request).execute();
    }

    /**
     * 提取回應內容
     */
    private String extractBody(Response response) throws IOException {
        try (ResponseBody body = response.body()) {
            if (body == null) {
                return "";
            }
            return body.string();
        }
    }
}
