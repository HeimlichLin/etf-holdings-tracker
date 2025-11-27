package com.etf.tracker.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.etf.tracker.config.AppConfig;
import com.etf.tracker.exception.StorageException;
import com.etf.tracker.model.DailySnapshot;
import com.etf.tracker.model.Holding;

/**
 * Excel 儲存服務
 * <p>
 * 使用 Apache POI 實作 Excel 檔案的讀寫功能
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
@Service
public class ExcelStorageService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelStorageService.class);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String SHEET_NAME = "Holdings";
    private static final String[] HEADERS = { "日期", "股票代號", "股票名稱", "股數", "權重(%)" };

    private final AppConfig appConfig;
    private final Path storagePath;

    public ExcelStorageService(AppConfig appConfig) {
        this.appConfig = appConfig;
        this.storagePath = Path.of(appConfig.getData().getStoragePath());
    }

    /**
     * 儲存每日快照
     *
     * @param snapshot 每日快照
     * @throws StorageException 如果儲存失敗
     */
    public void saveSnapshot(DailySnapshot snapshot) {
        validateSnapshot(snapshot);

        Path filePath = getFilePath();
        logger.info("儲存快照: 日期={}, 成分股數量={}, 檔案={}",
                snapshot.getDate(), snapshot.getTotalCount(), filePath);

        try {
            ensureDirectoryExists();

            Workbook workbook = loadOrCreateWorkbook(filePath);
            Sheet sheet = getOrCreateSheet(workbook);

            // 刪除該日期的舊資料
            deleteExistingData(sheet, snapshot.getDate());

            // 寫入新資料
            writeSnapshotData(sheet, snapshot);

            // 儲存檔案
            saveWorkbook(workbook, filePath);

            logger.info("快照儲存成功: {}", snapshot.getDate());

        } catch (IOException e) {
            logger.error("儲存快照失敗: {}", e.getMessage(), e);
            throw StorageException.writeError(filePath, e);
        }
    }

    /**
     * 取得指定日期的快照
     *
     * @param date 日期
     * @return 快照，如果不存在則為空
     */
    public Optional<DailySnapshot> getSnapshot(LocalDate date) {
        Path filePath = getFilePath();

        if (!Files.exists(filePath)) {
            return Optional.empty();
        }

        try (InputStream is = Files.newInputStream(filePath);
                Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                return Optional.empty();
            }

            List<Holding> holdings = new ArrayList<>();
            String dateStr = date.format(DATE_FORMATTER);
            boolean dateFound = false;

            for (Row row : sheet) {
                if (row.getRowNum() == 0)
                    continue; // 跳過標題

                Cell dateCell = row.getCell(0);
                if (dateCell != null && dateStr.equals(getCellStringValue(dateCell))) {
                    dateFound = true;
                    Holding holding = parseRow(row);
                    if (holding != null) {
                        holdings.add(holding);
                    }
                }
            }

            // 日期不存在於檔案中
            if (!dateFound) {
                return Optional.empty();
            }

            // 日期存在但可能沒有持倉（空快照）- 仍回傳有效快照
            DailySnapshot snapshot = DailySnapshot.builder()
                    .date(date)
                    .holdings(holdings)
                    .totalCount(holdings.size())
                    .totalWeight(calculateTotalWeight(holdings))
                    .build();

            return Optional.of(snapshot);

        } catch (IOException e) {
            logger.error("讀取快照失敗: {}", e.getMessage(), e);
            throw StorageException.readError(filePath, e);
        }
    }

    /**
     * 取得最新的快照
     *
     * @return 最新快照，如果沒有資料則為空
     */
    public Optional<DailySnapshot> getLatestSnapshot() {
        List<LocalDate> dates = getAvailableDates();
        if (dates.isEmpty()) {
            return Optional.empty();
        }
        return getSnapshot(dates.get(0)); // 第一個是最新的
    }

    /**
     * 取得所有可用日期（降序排列）
     *
     * @return 日期清單
     */
    public List<LocalDate> getAvailableDates() {
        Path filePath = getFilePath();

        if (!Files.exists(filePath)) {
            return Collections.emptyList();
        }

        try (InputStream is = Files.newInputStream(filePath);
                Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                return Collections.emptyList();
            }

            Set<LocalDate> dateSet = new TreeSet<>(Comparator.reverseOrder());

            for (Row row : sheet) {
                if (row.getRowNum() == 0)
                    continue;

                Cell dateCell = row.getCell(0);
                if (dateCell != null) {
                    String dateStr = getCellStringValue(dateCell);
                    try {
                        LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
                        dateSet.add(date);
                    } catch (Exception e) {
                        // 忽略無效日期
                    }
                }
            }

            return new ArrayList<>(dateSet);

        } catch (IOException e) {
            logger.error("讀取可用日期失敗: {}", e.getMessage(), e);
            throw StorageException.readError(filePath, e);
        }
    }

    /**
     * 計算指定日期之前的記錄數（用於清理預覽）
     *
     * @param cutoffDate 截止日期
     * @return 符合條件的記錄數
     */
    public int countRecordsBefore(LocalDate cutoffDate) {
        Path filePath = getFilePath();

        if (!Files.exists(filePath)) {
            return 0;
        }

        try (InputStream is = Files.newInputStream(filePath);
                Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheet(SHEET_NAME);

            if (sheet == null) {
                return 0;
            }

            int count = 0;

            for (Row row : sheet) {
                if (row.getRowNum() == 0)
                    continue;

                Cell dateCell = row.getCell(0);
                if (dateCell != null) {
                    String dateStr = getCellStringValue(dateCell);
                    try {
                        LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
                        // 刪除所有早於或等於 cutoffDate 的資料
                        if (date.compareTo(cutoffDate) <= 0) {
                            count++;
                        }
                    } catch (Exception e) {
                        // 忽略無效日期
                    }
                }
            }

            return count;

        } catch (IOException e) {
            logger.error("計算過期資料筆數失敗: {}", e.getMessage(), e);
            throw StorageException.readError(filePath, e);
        }
    }

    /**
     * 取得總記錄數（所有日期的成分股總筆數）
     *
     * @return 總記錄數
     */
    public int getTotalRecordCount() {
        Path filePath = getFilePath();

        if (!Files.exists(filePath)) {
            return 0;
        }

        try (InputStream is = Files.newInputStream(filePath);
                Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheet(SHEET_NAME);

            if (sheet == null) {
                return 0;
            }

            int count = 0;
            for (Row row : sheet) {
                if (row.getRowNum() == 0)
                    continue;
                count++;
            }

            return count;

        } catch (IOException e) {
            logger.error("計算總記錄數失敗: {}", e.getMessage(), e);
            throw StorageException.readError(filePath, e);
        }
    }

    /**
     * 刪除指定日期之前的資料
     *
     * @param cutoffDate 截止日期
     * @return 刪除的記錄數
     */
    public int deleteDataBefore(LocalDate cutoffDate) {
        Path filePath = getFilePath();

        if (!Files.exists(filePath)) {
            return 0;
        }

        try {
            Workbook workbook = loadOrCreateWorkbook(filePath);
            Sheet sheet = workbook.getSheet(SHEET_NAME);

            if (sheet == null) {
                return 0;
            }

            List<Integer> rowsToDelete = new ArrayList<>();
            String cutoffStr = cutoffDate.format(DATE_FORMATTER);

            for (Row row : sheet) {
                if (row.getRowNum() == 0)
                    continue;

                Cell dateCell = row.getCell(0);
                if (dateCell != null) {
                    String dateStr = getCellStringValue(dateCell);
                    try {
                        LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
                        // 刪除所有早於或等於 cutoffDate 的資料
                        if (date.compareTo(cutoffDate) <= 0) {
                            rowsToDelete.add(row.getRowNum());
                        }
                    } catch (Exception e) {
                        // 忽略無效日期
                    }
                }
            }

            // 從後往前刪除，避免索引問題
            Collections.reverse(rowsToDelete);
            for (int rowNum : rowsToDelete) {
                Row row = sheet.getRow(rowNum);
                if (row != null) {
                    sheet.removeRow(row);
                    // 如果不是最後一行，需要上移
                    int lastRowNum = sheet.getLastRowNum();
                    if (rowNum < lastRowNum) {
                        sheet.shiftRows(rowNum + 1, lastRowNum, -1);
                    }
                }
            }

            saveWorkbook(workbook, filePath);

            logger.info("已刪除 {} 筆過期資料（{}之前）", rowsToDelete.size(), cutoffDate);
            return rowsToDelete.size();

        } catch (IOException e) {
            logger.error("刪除過期資料失敗: {}", e.getMessage(), e);
            throw StorageException.writeError(filePath, e);
        }
    }

    /**
     * 取得儲存檔案路徑
     */
    private Path getFilePath() {
        return storagePath.resolve(appConfig.getData().getFileName());
    }

    /**
     * 確保儲存目錄存在
     */
    private void ensureDirectoryExists() throws IOException {
        if (!Files.exists(storagePath)) {
            Files.createDirectories(storagePath);
            logger.info("建立儲存目錄: {}", storagePath);
        }
    }

    /**
     * 載入或建立工作簿
     */
    private Workbook loadOrCreateWorkbook(Path filePath) throws IOException {
        if (Files.exists(filePath)) {
            try (InputStream is = Files.newInputStream(filePath)) {
                return new XSSFWorkbook(is);
            }
        }
        return new XSSFWorkbook();
    }

    /**
     * 取得或建立工作表
     */
    private Sheet getOrCreateSheet(Workbook workbook) {
        Sheet sheet = workbook.getSheet(SHEET_NAME);
        if (sheet == null) {
            sheet = workbook.createSheet(SHEET_NAME);
            createHeader(workbook, sheet);
        }
        return sheet;
    }

    /**
     * 建立標題列
     */
    private void createHeader(Workbook workbook, Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = createHeaderStyle(workbook);

        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    /**
     * 建立標題樣式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    /**
     * 刪除指定日期的舊資料
     */
    private void deleteExistingData(Sheet sheet, LocalDate date) {
        String dateStr = date.format(DATE_FORMATTER);
        List<Integer> rowsToDelete = new ArrayList<>();

        for (Row row : sheet) {
            if (row.getRowNum() == 0)
                continue;

            Cell dateCell = row.getCell(0);
            if (dateCell != null && dateStr.equals(getCellStringValue(dateCell))) {
                rowsToDelete.add(row.getRowNum());
            }
        }

        // 從後往前刪除
        Collections.reverse(rowsToDelete);
        for (int rowNum : rowsToDelete) {
            Row row = sheet.getRow(rowNum);
            if (row != null) {
                sheet.removeRow(row);
                int lastRowNum = sheet.getLastRowNum();
                if (rowNum < lastRowNum) {
                    sheet.shiftRows(rowNum + 1, lastRowNum, -1);
                }
            }
        }
    }

    /**
     * 寫入快照資料
     */
    private void writeSnapshotData(Sheet sheet, DailySnapshot snapshot) {
        String dateStr = snapshot.getDate().format(DATE_FORMATTER);
        int rowNum = sheet.getLastRowNum() + 1;

        if (snapshot.getHoldings().isEmpty()) {
            // 空持倉：寫入一行佔位符資料，標記該日期存在但無持倉
            Row row = sheet.createRow(rowNum);
            row.createCell(0).setCellValue(dateStr);
            row.createCell(1).setCellValue(""); // 空股票代號
            row.createCell(2).setCellValue(""); // 空股票名稱
            row.createCell(3).setCellValue(0); // 零股數
            row.createCell(4).setCellValue(0.0); // 零權重
        } else {
            for (Holding holding : snapshot.getHoldings()) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(dateStr);
                row.createCell(1).setCellValue(holding.getStockCode());
                row.createCell(2).setCellValue(holding.getStockName());
                row.createCell(3).setCellValue(holding.getShares());
                row.createCell(4).setCellValue(holding.getWeight().doubleValue());
            }
        }

        // 調整欄寬
        for (int i = 0; i < HEADERS.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * 儲存工作簿
     */
    private void saveWorkbook(Workbook workbook, Path filePath) throws IOException {
        try (OutputStream os = Files.newOutputStream(filePath)) {
            workbook.write(os);
        } finally {
            workbook.close();
        }
    }

    /**
     * 解析行資料為 Holding
     * 回傳 null 如果是空佔位符資料（空股票代號）
     */
    private Holding parseRow(Row row) {
        try {
            String stockCode = getCellStringValue(row.getCell(1));

            // 空股票代號代表佔位符資料（空持倉日期），跳過
            if (stockCode == null || stockCode.isEmpty()) {
                return null;
            }

            String stockName = getCellStringValue(row.getCell(2));
            Long shares = getCellLongValue(row.getCell(3));
            BigDecimal weight = getCellBigDecimalValue(row.getCell(4));

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

    /**
     * 取得儲存格字串值
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null)
            return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> "";
        };
    }

    /**
     * 取得儲存格 Long 值
     */
    private Long getCellLongValue(Cell cell) {
        if (cell == null)
            return 0L;
        return switch (cell.getCellType()) {
            case NUMERIC -> (long) cell.getNumericCellValue();
            case STRING -> Long.parseLong(cell.getStringCellValue().replaceAll("[,\\s]", ""));
            default -> 0L;
        };
    }

    /**
     * 取得儲存格 BigDecimal 值
     */
    private BigDecimal getCellBigDecimalValue(Cell cell) {
        if (cell == null)
            return BigDecimal.ZERO;
        return switch (cell.getCellType()) {
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
            case STRING -> new BigDecimal(cell.getStringCellValue().replaceAll("[%\\s]", ""));
            default -> BigDecimal.ZERO;
        };
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
     * 驗證快照
     */
    private void validateSnapshot(DailySnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("快照不可為 null");
        }
        if (snapshot.getDate() == null) {
            throw new IllegalArgumentException("快照日期不可為 null");
        }
    }
}
