package com.etf.tracker.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.etf.tracker.config.AppConfig;
import com.etf.tracker.model.DailySnapshot;
import com.etf.tracker.model.Holding;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import jakarta.annotation.PostConstruct;

/**
 * 混合儲存服務
 * <p>
 * 結合 Google Sheets（讀取）和本地 Excel（寫入）的混合模式：
 * <ul>
 * <li>讀取：優先從公開的 Google Sheets 讀取，失敗時回退到本地 Excel</li>
 * <li>寫入：使用本地 Excel 儲存</li>
 * </ul>
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
@Service
@Primary
@ConditionalOnProperty(name = "app.google-sheets.enabled", havingValue = "true")
public class HybridStorageService implements StorageService {

    private static final Logger logger = LoggerFactory.getLogger(HybridStorageService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Google Sheets 公開 API URL 模板
    // 使用 gviz 查詢 API，可以讀取公開的 Sheets
    private static final String SHEETS_API_URL_TEMPLATE = "https://docs.google.com/spreadsheets/d/%s/gviz/tq?tqx=out:json&sheet=%s";

    private final AppConfig appConfig;
    private final ExcelStorageService excelStorageService;
    private final HttpClient httpClient;
    private final Gson gson;

    private String sheetsApiUrl;

    // 追蹤最後一次讀取的資料來源
    private volatile boolean lastReadFromGoogleSheets = false;

    public HybridStorageService(AppConfig appConfig, ExcelStorageService excelStorageService) {
        this.appConfig = appConfig;
        this.excelStorageService = excelStorageService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.gson = new Gson();
    }

    @PostConstruct
    public void init() {
        var config = appConfig.getGoogleSheets();
        this.sheetsApiUrl = String.format(SHEETS_API_URL_TEMPLATE,
                config.getSpreadsheetId(),
                config.getSheetName());
        logger.info("混合儲存服務初始化成功: Google Sheets ID={}, 本地 Excel={}",
                config.getSpreadsheetId(),
                appConfig.getData().getStoragePath() + "/" + appConfig.getData().getFileName());
    }

    // ==================== 資料來源判斷 ====================

    /**
     * 判斷目前是否為唯讀模式
     * <p>
     * 當最後一次讀取來自 Google Sheets 時，為唯讀模式
     * </p>
     */
    @Override
    public boolean isReadOnly() {
        return lastReadFromGoogleSheets;
    }

    /**
     * 取得資料來源資訊
     */
    @Override
    public String getDataSourceInfo() {
        if (lastReadFromGoogleSheets) {
            return "Google Sheets (唯讀)";
        }
        return "本地 Excel";
    }

    // ==================== 寫入操作（使用本地 Excel）====================

    @Override
    public void saveSnapshot(DailySnapshot snapshot) {
        logger.info("儲存快照到本地 Excel: 日期={}, 成分股數量={}",
                snapshot.getDate(), snapshot.getTotalCount());
        excelStorageService.saveSnapshot(snapshot);
    }

    @Override
    public int deleteDataBefore(LocalDate cutoffDate) {
        logger.info("從本地 Excel 刪除過期資料: cutoffDate={}", cutoffDate);
        return excelStorageService.deleteDataBefore(cutoffDate);
    }

    // ==================== 讀取操作（優先 Google Sheets，回退到本地）====================

    @Override
    public Optional<DailySnapshot> getSnapshot(LocalDate date) {
        try {
            Optional<DailySnapshot> result = getSnapshotFromGoogleSheets(date);
            if (result.isPresent()) {
                logger.debug("從 Google Sheets 讀取快照成功: {}", date);
                lastReadFromGoogleSheets = true;
                return result;
            }
        } catch (Exception e) {
            logger.warn("從 Google Sheets 讀取失敗，回退到本地 Excel: {}", e.getMessage());
        }
        lastReadFromGoogleSheets = false;
        return excelStorageService.getSnapshot(date);
    }

    @Override
    public Optional<DailySnapshot> getLatestSnapshot() {
        try {
            List<LocalDate> dates = getAvailableDatesFromGoogleSheets();
            if (!dates.isEmpty()) {
                Optional<DailySnapshot> result = getSnapshotFromGoogleSheets(dates.get(0));
                if (result.isPresent()) {
                    lastReadFromGoogleSheets = true;
                    return result;
                }
            }
        } catch (Exception e) {
            logger.warn("從 Google Sheets 讀取最新快照失敗，回退到本地 Excel: {}", e.getMessage());
        }
        lastReadFromGoogleSheets = false;
        return excelStorageService.getLatestSnapshot();
    }

    @Override
    public List<LocalDate> getAvailableDates() {
        try {
            List<LocalDate> dates = getAvailableDatesFromGoogleSheets();
            if (!dates.isEmpty()) {
                logger.debug("從 Google Sheets 讀取可用日期成功: {} 個日期", dates.size());
                lastReadFromGoogleSheets = true;
                return dates;
            }
        } catch (Exception e) {
            logger.warn("從 Google Sheets 讀取可用日期失敗，回退到本地 Excel: {}", e.getMessage());
        }
        lastReadFromGoogleSheets = false;
        return excelStorageService.getAvailableDates();
    }

    @Override
    public int countRecordsBefore(LocalDate cutoffDate) {
        try {
            List<List<String>> allData = fetchAllDataFromGoogleSheets();
            int count = 0;
            for (List<String> row : allData) {
                if (row.isEmpty())
                    continue;
                try {
                    LocalDate date = LocalDate.parse(row.get(0), DATE_FORMATTER);
                    // 使用 isBefore 嚴格比較：只有早於截止日的才計入（不含截止日當天）
                    if (date.isBefore(cutoffDate)) {
                        count++;
                    }
                } catch (Exception e) {
                    // 忽略無效日期
                }
            }
            return count;
        } catch (Exception e) {
            logger.warn("從 Google Sheets 計算過期資料筆數失敗，回退到本地 Excel: {}", e.getMessage());
        }
        return excelStorageService.countRecordsBefore(cutoffDate);
    }

    @Override
    public int getTotalRecordCount() {
        try {
            List<List<String>> allData = fetchAllDataFromGoogleSheets();
            return allData.size();
        } catch (Exception e) {
            logger.warn("從 Google Sheets 計算總記錄數失敗，回退到本地 Excel: {}", e.getMessage());
        }
        return excelStorageService.getTotalRecordCount();
    }

    // ==================== Google Sheets 公開 API 讀取方法 ====================

    /**
     * 從 Google Sheets 獲取所有資料
     * 使用 Google Visualization API (gviz)，可以讀取公開的 Sheets
     */
    private List<List<String>> fetchAllDataFromGoogleSheets() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(sheetsApiUrl))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Google Sheets API 請求失敗: HTTP " + response.statusCode());
        }

        return parseGvizResponse(response.body());
    }

    /**
     * 解析 Google Visualization API 回應
     * 回應格式: google.visualization.Query.setResponse({...})
     */
    private List<List<String>> parseGvizResponse(String responseBody) {
        List<List<String>> result = new ArrayList<>();

        // 移除 JSONP 包裝: google.visualization.Query.setResponse(...)
        String jsonStr = responseBody;
        int startIdx = responseBody.indexOf("(");
        int endIdx = responseBody.lastIndexOf(")");
        if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
            jsonStr = responseBody.substring(startIdx + 1, endIdx);
        }

        JsonObject json = gson.fromJson(jsonStr, JsonObject.class);
        JsonObject table = json.getAsJsonObject("table");
        if (table == null) {
            return result;
        }

        JsonArray rows = table.getAsJsonArray("rows");
        if (rows == null) {
            return result;
        }

        // gviz API 的 rows 陣列直接是資料列（標題在 cols.label 中）
        for (int i = 0; i < rows.size(); i++) {
            JsonObject row = rows.get(i).getAsJsonObject();
            JsonArray cells = row.getAsJsonArray("c");
            if (cells == null)
                continue;

            List<String> rowData = new ArrayList<>();
            for (JsonElement cell : cells) {
                if (cell == null || cell.isJsonNull()) {
                    rowData.add("");
                } else {
                    JsonObject cellObj = cell.getAsJsonObject();
                    JsonElement v = cellObj.get("v");
                    if (v == null || v.isJsonNull()) {
                        rowData.add("");
                    } else {
                        rowData.add(v.getAsString());
                    }
                }
            }
            result.add(rowData);
        }

        return result;
    }

    private List<LocalDate> getAvailableDatesFromGoogleSheets() throws IOException, InterruptedException {
        List<List<String>> allData = fetchAllDataFromGoogleSheets();
        Set<LocalDate> dateSet = new TreeSet<>(Comparator.reverseOrder());

        for (List<String> row : allData) {
            if (row.isEmpty())
                continue;
            try {
                LocalDate date = LocalDate.parse(row.get(0), DATE_FORMATTER);
                dateSet.add(date);
            } catch (Exception e) {
                // 忽略無效日期
            }
        }

        return new ArrayList<>(dateSet);
    }

    private Optional<DailySnapshot> getSnapshotFromGoogleSheets(LocalDate date)
            throws IOException, InterruptedException {
        List<List<String>> allData = fetchAllDataFromGoogleSheets();
        String dateStr = date.format(DATE_FORMATTER);
        List<Holding> holdings = new ArrayList<>();
        boolean dateFound = false;

        for (List<String> row : allData) {
            if (row.isEmpty())
                continue;

            if (dateStr.equals(row.get(0))) {
                dateFound = true;
                Holding holding = parseRow(row);
                if (holding != null) {
                    holdings.add(holding);
                }
            }
        }

        if (!dateFound) {
            return Optional.empty();
        }

        DailySnapshot snapshot = DailySnapshot.builder()
                .date(date)
                .holdings(holdings)
                .totalCount(holdings.size())
                .totalWeight(calculateTotalWeight(holdings))
                .build();

        return Optional.of(snapshot);
    }

    private Holding parseRow(List<String> row) {
        try {
            if (row.size() < 5)
                return null;

            String stockCode = row.get(1);
            if (stockCode == null || stockCode.isEmpty()) {
                return null;
            }

            String stockName = row.get(2);
            Long shares = parseShares(row.get(3));
            BigDecimal weight = parseWeight(row.get(4));

            return Holding.builder()
                    .stockCode(stockCode)
                    .stockName(stockName)
                    .shares(shares)
                    .weight(weight)
                    .build();
        } catch (Exception e) {
            logger.warn("解析行資料失敗: {}", e.getMessage());
            return null;
        }
    }

    private Long parseShares(String value) {
        if (value == null || value.isEmpty())
            return 0L;
        String str = value.replaceAll("[,\\s]", "");
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            try {
                return (long) Double.parseDouble(str);
            } catch (NumberFormatException e2) {
                return 0L;
            }
        }
    }

    private BigDecimal parseWeight(String value) {
        if (value == null || value.isEmpty())
            return BigDecimal.ZERO;
        String str = value.replaceAll("[%\\s]", "");
        try {
            return new BigDecimal(str);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal calculateTotalWeight(List<Holding> holdings) {
        return holdings.stream()
                .map(Holding::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
