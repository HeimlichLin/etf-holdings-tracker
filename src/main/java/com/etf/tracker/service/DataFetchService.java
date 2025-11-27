package com.etf.tracker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.etf.tracker.config.AppConfig;
import com.etf.tracker.exception.DataFetchException;
import com.etf.tracker.model.DailySnapshot;
import com.etf.tracker.scraper.EzMoneyScraperStrategy;
import com.etf.tracker.scraper.PlaywrightWebClient;

/**
 * 資料抓取服務
 * <p>
 * 整合 HTTP 客戶端與網頁擷取策略，提供持倉資料抓取功能
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
@Service
public class DataFetchService {

    private static final Logger logger = LoggerFactory.getLogger(DataFetchService.class);

    private final PlaywrightWebClient webClient;
    private final EzMoneyScraperStrategy scraperStrategy;
    private final AppConfig appConfig;

    public DataFetchService(PlaywrightWebClient webClient,
            EzMoneyScraperStrategy scraperStrategy,
            AppConfig appConfig) {
        this.webClient = webClient;
        this.scraperStrategy = scraperStrategy;
        this.appConfig = appConfig;
    }

    /**
     * 抓取最新持倉資料
     * <p>
     * 從配置的目標網站抓取 ETF 00981A 的最新持倉資料
     * </p>
     *
     * @return 每日快照
     * @throws DataFetchException 如果抓取或解析失敗
     */
    public DailySnapshot fetchLatestHoldings() {
        String targetUrl = appConfig.getScraper().getTargetUrl();
        logger.info("開始抓取持倉資料: {}", targetUrl);

        try {
            // 1. 取得 HTML 內容
            long startTime = System.currentTimeMillis();
            String html = webClient.fetchHtml(targetUrl);
            long fetchTime = System.currentTimeMillis() - startTime;
            logger.debug("HTML 抓取完成，耗時 {} ms，大小 {} bytes", fetchTime, html.length());

            // 2. 解析 HTML
            startTime = System.currentTimeMillis();
            DailySnapshot snapshot = scraperStrategy.parseHoldings(html);
            long parseTime = System.currentTimeMillis() - startTime;
            logger.debug("HTML 解析完成，耗時 {} ms", parseTime);

            logger.info("成功抓取持倉資料: 日期={}, 成分股數量={}, 總權重={}%",
                    snapshot.getDate(),
                    snapshot.getTotalCount(),
                    snapshot.getTotalWeight());

            return snapshot;

        } catch (DataFetchException e) {
            logger.error("抓取持倉資料失敗: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("抓取持倉資料時發生未預期錯誤: {}", e.getMessage(), e);
            throw new DataFetchException("抓取持倉資料失敗: " + e.getMessage(), e);
        }
    }

    /**
     * 檢查資料來源是否可用
     *
     * @return true 如果資料來源可用
     */
    public boolean isDataSourceAvailable() {
        try {
            String targetUrl = appConfig.getScraper().getTargetUrl();
            String html = webClient.fetchHtml(targetUrl);
            return html != null && !html.isEmpty();
        } catch (Exception e) {
            logger.warn("資料來源檢查失敗: {}", e.getMessage());
            return false;
        }
    }
}
