package com.etf.tracker.scraper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.etf.tracker.exception.DataFetchException;
import com.etf.tracker.model.DailySnapshot;
import com.etf.tracker.model.Holding;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * EzMoney 網站擷取策略
 * <p>
 * 使用 Jsoup 解析 ezmoney.com.tw 網站的 ETF 持倉頁面
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
@Component
public class EzMoneyScraperStrategy {

    private static final Logger logger = LoggerFactory.getLogger(EzMoneyScraperStrategy.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}[-/]\\d{2}[-/]\\d{2}");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("[yyyy-MM-dd][yyyy/MM/dd]");

    // 表格選擇器 - 支援多種可能的結構
    private static final String[] TABLE_SELECTORS = {
            "table:contains(股票代號) tbody tr",
            "div#asset table tbody tr",
            "table#holdingsTable tbody tr",
            "table.holdings-table tbody tr",
            "div.etf-holdings table tbody tr",
            "table tbody tr"
    };

    /**
     * 解析 HTML 取得持倉快照
     *
     * @param html HTML 內容
     * @return 每日快照
     * @throws DataFetchException 如果解析失敗
     */
    public DailySnapshot parseHoldings(String html) {
        if (html == null) {
            throw new IllegalArgumentException("HTML 內容不可為 null");
        }

        if (html.isBlank()) {
            throw DataFetchException.parseError("", new IllegalArgumentException("HTML 內容為空"));
        }

        try {
            Document doc = Jsoup.parse(html);

            // 解析資料日期
            LocalDate date = extractDate(doc);

            // 解析持倉資料
            List<Holding> holdings = extractHoldingsFromJson(html);
            if (holdings.isEmpty()) {
                holdings = extractHoldings(doc);
            }

            // 建立快照
            DailySnapshot snapshot = DailySnapshot.builder()
                    .date(date)
                    .holdings(holdings)
                    .totalCount(holdings.size())
                    .totalWeight(calculateTotalWeight(holdings))
                    .build();

            logger.info("成功解析持倉資料: 日期={}, 成分股數量={}", date, holdings.size());
            return snapshot;

        } catch (DataFetchException e) {
            throw e;
        } catch (Exception e) {
            logger.error("解析 HTML 失敗: {}", e.getMessage(), e);
            throw DataFetchException.parseError("", e);
        }
    }

    /**
     * 從 HTML 中的 JavaScript 變數提取持倉資料
     */
    private List<Holding> extractHoldingsFromJson(String html) {
        List<Holding> holdings = new ArrayList<>();
        // 尋找 var assetDB = [...];
        Pattern pattern = Pattern.compile("var\\s+assetDB\\s*=\s*(\\[[\\s\\S]*?\\]);");
        Matcher matcher = pattern.matcher(html);

        if (matcher.find()) {
            String json = matcher.group(1);
            try {
                JsonNode root = objectMapper.readTree(json);
                if (root.isArray()) {
                    for (JsonNode asset : root) {
                        // 尋找股票資產 (AssetCode: "ST")
                        if (asset.has("AssetCode") && "ST".equals(asset.get("AssetCode").asText())) {
                            JsonNode details = asset.get("Details");
                            if (details != null && details.isArray()) {
                                for (JsonNode item : details) {
                                    Holding holding = new Holding();
                                    if (item.has("DetailCode")) {
                                        holding.setStockCode(item.get("DetailCode").asText());
                                    }
                                    if (item.has("DetailName")) {
                                        holding.setStockName(item.get("DetailName").asText());
                                    }
                                    if (item.has("Share")) {
                                        holding.setShares(item.get("Share").asLong());
                                    }
                                    if (item.has("NavRate")) {
                                        holding.setWeight(new BigDecimal(item.get("NavRate").asText()));
                                    }
                                    holdings.add(holding);
                                }
                            }
                        }
                    }
                }
                if (!holdings.isEmpty()) {
                    logger.info("成功從 JSON 提取 {} 筆持倉資料", holdings.size());
                }
            } catch (Exception e) {
                logger.warn("解析 assetDB JSON 失敗", e);
            }
        }
        return holdings;
    }

    /**
     * 從文件中提取日期
     */
    private LocalDate extractDate(Document doc) {
        // 嘗試從多個位置提取日期
        String[] dateSelectors = {
                "h5:contains(資料日期)",
                "p:contains(資料日期)",
                "span.date",
                "div.update-date",
                "td:contains(資料日期)"
        };

        for (String selector : dateSelectors) {
            Elements elements = doc.select(selector);
            for (Element element : elements) {
                String text = element.text();
                Matcher matcher = DATE_PATTERN.matcher(text);
                if (matcher.find()) {
                    try {
                        return LocalDate.parse(matcher.group(), DATE_FORMATTER);
                    } catch (DateTimeParseException e) {
                        logger.warn("日期解析失敗: {}", matcher.group());
                    }
                }
            }
        }

        // 如果找不到日期，使用今天
        logger.warn("無法從頁面提取日期，使用今日日期");
        return LocalDate.now();
    }

    /**
     * 從文件中提取持倉資料
     */
    private List<Holding> extractHoldings(Document doc) {
        List<Holding> holdings = new ArrayList<>();

        // 嘗試不同的表格選擇器
        Elements rows = null;
        for (String selector : TABLE_SELECTORS) {
            rows = doc.select(selector);
            if (!rows.isEmpty()) {
                logger.debug("使用選擇器: {}, 找到 {} 行", selector, rows.size());
                break;
            }
        }

        if (rows == null || rows.isEmpty()) {
            // 檢查是否為空表格（合法情況）
            if (isEmptyTable(doc)) {
                logger.info("持倉表格為空");
                return holdings;
            }
            throw new DataFetchException("無法找到持倉表格", "TABLE_NOT_FOUND", null, null);
        }

        for (Element row : rows) {
            Elements cells = row.select("td");
            if (cells.size() < 4) {
                continue; // 跳過不完整的行
            }

            try {
                Holding holding = parseRow(cells);
                if (holding != null) {
                    holdings.add(holding);
                }
            } catch (Exception e) {
                logger.warn("解析行失敗: {}", e.getMessage());
            }
        }

        return holdings;
    }

    /**
     * 解析單一表格行
     */
    private Holding parseRow(Elements cells) {
        // 根據欄位數量判斷格式
        // 格式 1: 序號, 代號, 名稱, 股數, 權重
        // 格式 2: 代號, 名稱, 股數, 權重

        int offset = cells.size() >= 5 ? 1 : 0;

        String stockCode = cells.get(offset).text().trim();
        String stockName = cells.get(offset + 1).text().trim();
        String sharesText = cells.get(offset + 2).text().trim();
        String weightText = cells.get(offset + 3).text().trim();

        // 跳過表頭或無效行
        if (stockCode.isEmpty() || stockCode.equals("股票代號") || stockCode.equals("代號")) {
            return null;
        }

        Long shares = parseShares(sharesText);
        BigDecimal weight = parseWeight(weightText);

        return Holding.builder()
                .stockCode(stockCode)
                .stockName(stockName)
                .shares(shares)
                .weight(weight)
                .build();
    }

    /**
     * 解析股數（處理逗號分隔的數字）
     */
    private Long parseShares(String text) {
        if (text == null || text.isEmpty()) {
            return 0L;
        }
        // 移除逗號和空白
        String cleaned = text.replaceAll("[,\\s]", "");
        try {
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            logger.warn("無法解析股數: {}", text);
            return 0L;
        }
    }

    /**
     * 解析權重百分比
     */
    private BigDecimal parseWeight(String text) {
        if (text == null || text.isEmpty()) {
            return BigDecimal.ZERO;
        }
        // 移除百分比符號和空白
        String cleaned = text.replaceAll("[%\\s]", "");
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            logger.warn("無法解析權重: {}", text);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 計算總權重
     */
    private BigDecimal calculateTotalWeight(List<Holding> holdings) {
        return holdings.stream()
                .map(Holding::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 檢查是否為空表格
     */
    private boolean isEmptyTable(Document doc) {
        // 檢查是否有「無資料」或「目前無資料」等文字
        String text = doc.text();
        return text.contains("無資料") || text.contains("目前無資料") || text.contains("No data");
    }
}
