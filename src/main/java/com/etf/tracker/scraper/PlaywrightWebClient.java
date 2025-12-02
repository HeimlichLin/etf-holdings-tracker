package com.etf.tracker.scraper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;

@Component
public class PlaywrightWebClient {

    private static final Logger logger = LoggerFactory.getLogger(PlaywrightWebClient.class);

    public String fetchHtml(String url) {
        logger.info("使用 Playwright 抓取 URL: {}", url);
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext();
            Page page = context.newPage();

            page.navigate(url);

            // 等待頁面載入
            page.waitForLoadState();

            // 嘗試點擊「基金投資組合」頁籤
            try {
                // 使用文字定位，這與我們在 agent 工具中看到的一致
                Locator holdingsTab = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("基金投資組合"));
                if (holdingsTab.isVisible()) {
                    logger.info("點擊「基金投資組合」頁籤");
                    holdingsTab.click();

                    // 等待表格出現
                    // 根據之前的觀察，表格在 #asset 區塊內
                    page.waitForSelector("#asset table", new Page.WaitForSelectorOptions().setTimeout(5000));
                } else {
                    logger.warn("找不到「基金投資組合」頁籤");
                }
            } catch (Exception e) {
                logger.warn("切換到持倉頁籤時發生錯誤 (可能是已經在該頁面或選擇器不匹配): {}", e.getMessage());
            }

            // 獲取渲染後的 HTML
            String content = page.content();
            logger.debug("成功取得 HTML 內容，長度: {}", content.length());

            return content;

        } catch (Exception e) {
            logger.error("Playwright 抓取失敗: {}", e.getMessage(), e);
            throw new RuntimeException("Playwright 抓取失敗", e);
        }
    }
}
