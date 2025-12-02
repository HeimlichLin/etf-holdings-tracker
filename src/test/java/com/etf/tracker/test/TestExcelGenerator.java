package com.etf.tracker.test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * 測試用 Excel 檔案產生器
 * <p>
 * 用於產生測試用的持倉資料 Excel 檔案
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
public class TestExcelGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 測試用持倉資料
     */
    public record TestHolding(
            LocalDate date,
            String stockCode,
            String stockName,
            long shares,
            BigDecimal weight) {
    }

    /**
     * 產生測試用 Excel 檔案
     *
     * @param filePath 檔案路徑
     * @param holdings 持倉資料清單
     * @throws IOException 如果檔案寫入失敗
     */
    public static void generateTestExcel(Path filePath, List<TestHolding> holdings) throws IOException {
        // 確保目錄存在
        Files.createDirectories(filePath.getParent());

        try (Workbook workbook = new XSSFWorkbook();
                FileOutputStream fos = new FileOutputStream(filePath.toFile())) {

            Sheet sheet = workbook.createSheet("Holdings");

            // 建立標題列樣式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // 建立日期格式
            CellStyle dateStyle = workbook.createCellStyle();
            DataFormat dateFormat = workbook.createDataFormat();
            dateStyle.setDataFormat(dateFormat.getFormat("yyyy-MM-dd"));

            // 建立數字格式
            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.setDataFormat(dateFormat.getFormat("#,##0"));

            // 建立權重格式
            CellStyle weightStyle = workbook.createCellStyle();
            weightStyle.setDataFormat(dateFormat.getFormat("0.0000"));

            // 寫入標題列
            Row headerRow = sheet.createRow(0);
            String[] headers = { "日期", "股票代號", "股票名稱", "股數", "權重(%)" };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 寫入資料
            int rowNum = 1;
            for (TestHolding holding : holdings) {
                Row row = sheet.createRow(rowNum++);

                // 日期
                Cell dateCell = row.createCell(0);
                dateCell.setCellValue(holding.date().format(DATE_FORMATTER));

                // 股票代號
                Cell codeCell = row.createCell(1);
                codeCell.setCellValue(holding.stockCode());

                // 股票名稱
                Cell nameCell = row.createCell(2);
                nameCell.setCellValue(holding.stockName());

                // 股數
                Cell sharesCell = row.createCell(3);
                sharesCell.setCellValue(holding.shares());
                sharesCell.setCellStyle(numberStyle);

                // 權重
                Cell weightCell = row.createCell(4);
                weightCell.setCellValue(holding.weight().doubleValue());
                weightCell.setCellStyle(weightStyle);
            }

            // 調整欄寬
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(fos);
        }
    }

    /**
     * 建立範例測試資料
     *
     * @return 測試用持倉資料清單
     */
    public static List<TestHolding> createSampleData() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        return List.of(
                // 今日資料
                new TestHolding(today, "2330", "台積電", 1234567L, new BigDecimal("12.3456")),
                new TestHolding(today, "2317", "鴻海", 987654L, new BigDecimal("8.7654")),
                new TestHolding(today, "2454", "聯發科", 456789L, new BigDecimal("6.5432")),
                new TestHolding(today, "2412", "中華電", 345678L, new BigDecimal("5.4321")),
                new TestHolding(today, "3711", "日月光投控", 234567L, new BigDecimal("4.3210")),

                // 昨日資料
                new TestHolding(yesterday, "2330", "台積電", 1230000L, new BigDecimal("12.3000")),
                new TestHolding(yesterday, "2317", "鴻海", 990000L, new BigDecimal("8.8000")),
                new TestHolding(yesterday, "2454", "聯發科", 450000L, new BigDecimal("6.5000")),
                new TestHolding(yesterday, "2412", "中華電", 350000L, new BigDecimal("5.5000")),
                new TestHolding(yesterday, "2308", "台達電", 200000L, new BigDecimal("4.0000")) // 今日已剔除
        );
    }

    /**
     * 建立空的測試資料 Excel
     *
     * @param filePath 檔案路徑
     * @throws IOException 如果檔案寫入失敗
     */
    public static void generateEmptyExcel(Path filePath) throws IOException {
        generateTestExcel(filePath, List.of());
    }
}
