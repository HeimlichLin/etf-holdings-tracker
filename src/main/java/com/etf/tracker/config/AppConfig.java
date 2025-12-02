package com.etf.tracker.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 應用程式配置類別
 * <p>
 * 讀取 application.yml 中的自訂配置
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    private DataConfig data = new DataConfig();
    private ScraperConfig scraper = new ScraperConfig();
    private HttpClientConfig httpClient = new HttpClientConfig();

    // Getters and Setters

    public DataConfig getData() {
        return data;
    }

    public void setData(DataConfig data) {
        this.data = data;
    }

    public ScraperConfig getScraper() {
        return scraper;
    }

    public void setScraper(ScraperConfig scraper) {
        this.scraper = scraper;
    }

    public HttpClientConfig getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(HttpClientConfig httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * 資料儲存配置
     */
    public static class DataConfig {
        /** 資料儲存路徑 */
        private String storagePath = "./data";

        /** Excel 檔案名稱 */
        private String fileName = "holdings.xlsx";

        /** 資料保留天數 */
        private int retentionDays = 90;

        public String getStoragePath() {
            return storagePath;
        }

        public void setStoragePath(String storagePath) {
            this.storagePath = storagePath;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public int getRetentionDays() {
            return retentionDays;
        }

        public void setRetentionDays(int retentionDays) {
            this.retentionDays = retentionDays;
        }

        /**
         * 取得完整檔案路徑
         *
         * @return 完整檔案路徑
         */
        public String getFullFilePath() {
            return storagePath + "/" + fileName;
        }
    }

    /**
     * 網頁擷取配置
     */
    public static class ScraperConfig {
        /** 目標網站 URL */
        private String targetUrl = "https://www.ezmoney.com.tw/ETF/Fund/Info?FundCode=49YTW";

        /** 請求逾時時間（秒） */
        private int timeoutSeconds = 10;

        /** 最大重試次數 */
        private int maxRetries = 3;

        /** 重試間隔時間（秒） */
        private List<Integer> retryDelays = List.of(2, 4, 8);

        /** User-Agent */
        private String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

        public String getTargetUrl() {
            return targetUrl;
        }

        public void setTargetUrl(String targetUrl) {
            this.targetUrl = targetUrl;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public List<Integer> getRetryDelays() {
            return retryDelays;
        }

        public void setRetryDelays(List<Integer> retryDelays) {
            this.retryDelays = retryDelays;
        }

        public String getUserAgent() {
            return userAgent;
        }

        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }

        /**
         * 取得指定重試次數的延遲時間
         *
         * @param retryCount 重試次數（從 0 開始）
         * @return 延遲時間（秒）
         */
        public int getRetryDelay(int retryCount) {
            if (retryDelays == null || retryDelays.isEmpty()) {
                return 2; // 預設 2 秒
            }
            int index = Math.min(retryCount, retryDelays.size() - 1);
            return retryDelays.get(index);
        }
    }

    /**
     * HTTP 客戶端配置
     */
    public static class HttpClientConfig {
        /** 最大閒置連線數 */
        private int maxIdleConnections = 5;

        /** 連線存活時間（秒） */
        private int keepAliveDurationSeconds = 300;

        /** 連線逾時時間（秒） */
        private int connectTimeoutSeconds = 10;

        /** 讀取逾時時間（秒） */
        private int readTimeoutSeconds = 30;

        /** 寫入逾時時間（秒） */
        private int writeTimeoutSeconds = 30;

        public int getMaxIdleConnections() {
            return maxIdleConnections;
        }

        public void setMaxIdleConnections(int maxIdleConnections) {
            this.maxIdleConnections = maxIdleConnections;
        }

        public int getKeepAliveDurationSeconds() {
            return keepAliveDurationSeconds;
        }

        public void setKeepAliveDurationSeconds(int keepAliveDurationSeconds) {
            this.keepAliveDurationSeconds = keepAliveDurationSeconds;
        }

        public int getConnectTimeoutSeconds() {
            return connectTimeoutSeconds;
        }

        public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
            this.connectTimeoutSeconds = connectTimeoutSeconds;
        }

        public int getReadTimeoutSeconds() {
            return readTimeoutSeconds;
        }

        public void setReadTimeoutSeconds(int readTimeoutSeconds) {
            this.readTimeoutSeconds = readTimeoutSeconds;
        }

        public int getWriteTimeoutSeconds() {
            return writeTimeoutSeconds;
        }

        public void setWriteTimeoutSeconds(int writeTimeoutSeconds) {
            this.writeTimeoutSeconds = writeTimeoutSeconds;
        }
    }
}
