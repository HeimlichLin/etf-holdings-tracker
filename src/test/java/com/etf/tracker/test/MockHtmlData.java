package com.etf.tracker.test;

import java.time.LocalDate;

/**
 * 測試用模擬 HTML 資料
 * <p>
 * 提供模擬 ezmoney.com.tw 網站 HTML 結構的測試資料
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
public final class MockHtmlData {

    private MockHtmlData() {
        // 禁止實例化
    }

    /**
     * 產生模擬的 ETF 持倉 HTML
     *
     * @return 模擬 HTML 字串
     */
    public static String createMockHoldingsHtml() {
        return createMockHoldingsHtml(LocalDate.now());
    }

    /**
     * 產生指定日期的模擬 ETF 持倉 HTML
     *
     * @param date 資料日期
     * @return 模擬 HTML 字串
     */
    public static String createMockHoldingsHtml(LocalDate date) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>00981A 成分股明細</title>
                </head>
                <body>
                    <div class="etf-holdings">
                        <h2>00981A 成分股明細</h2>
                        <p>資料日期：%s</p>
                        <table class="holdings-table" id="holdingsTable">
                            <thead>
                                <tr>
                                    <th>序號</th>
                                    <th>股票代號</th>
                                    <th>股票名稱</th>
                                    <th>持股股數</th>
                                    <th>權重(%%)</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td>1</td>
                                    <td>2330</td>
                                    <td>台積電</td>
                                    <td>1,234,567</td>
                                    <td>12.3456</td>
                                </tr>
                                <tr>
                                    <td>2</td>
                                    <td>2317</td>
                                    <td>鴻海</td>
                                    <td>987,654</td>
                                    <td>8.7654</td>
                                </tr>
                                <tr>
                                    <td>3</td>
                                    <td>2454</td>
                                    <td>聯發科</td>
                                    <td>456,789</td>
                                    <td>6.5432</td>
                                </tr>
                                <tr>
                                    <td>4</td>
                                    <td>2412</td>
                                    <td>中華電</td>
                                    <td>345,678</td>
                                    <td>5.4321</td>
                                </tr>
                                <tr>
                                    <td>5</td>
                                    <td>3711</td>
                                    <td>日月光投控</td>
                                    <td>234,567</td>
                                    <td>4.3210</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </body>
                </html>
                """.formatted(date.toString());
    }

    /**
     * 產生空的持倉表格 HTML
     *
     * @return 空表格 HTML 字串
     */
    public static String createEmptyHoldingsHtml() {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>00981A 成分股明細</title>
                </head>
                <body>
                    <div class="etf-holdings">
                        <h2>00981A 成分股明細</h2>
                        <p>目前無資料</p>
                        <table class="holdings-table" id="holdingsTable">
                            <thead>
                                <tr>
                                    <th>序號</th>
                                    <th>股票代號</th>
                                    <th>股票名稱</th>
                                    <th>持股股數</th>
                                    <th>權重(%%)</th>
                                </tr>
                            </thead>
                            <tbody>
                            </tbody>
                        </table>
                    </div>
                </body>
                </html>
                """;
    }

    /**
     * 產生錯誤頁面 HTML
     *
     * @return 錯誤頁面 HTML 字串
     */
    public static String createErrorPageHtml() {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>錯誤</title>
                </head>
                <body>
                    <h1>發生錯誤</h1>
                    <p>無法取得資料，請稍後再試。</p>
                </body>
                </html>
                """;
    }

    /**
     * 產生非預期結構的 HTML（用於測試解析錯誤處理）
     *
     * @return 非預期結構 HTML 字串
     */
    public static String createMalformedHtml() {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>00981A</title>
                </head>
                <body>
                    <div class="different-structure">
                        <p>這不是預期的頁面結構</p>
                    </div>
                </body>
                </html>
                """;
    }
}
