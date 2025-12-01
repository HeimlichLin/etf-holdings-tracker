package com.etf.tracker.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import com.etf.tracker.util.DataRecoveryScript.HoldingData;

/**
 * 資料恢復腳本 - 將 2025-11-26 的 ETF 持股資料補回 Excel
 */
public class DataRecoveryScript {

    private static final String FILE_PATH = "D:/JAVA_WORKSPACE/COMMON/etf-holdings-tracker/data/holdings.xlsx";
    private static final String SHEET_NAME = "Holdings";
    private static final String TARGET_DATE = "2025-11-26";

    @Test
    public void recoverDataTest() {
        System.out.println("開始資料恢復...");

        // 準備要補回的資料 (從截圖整理)
        List<HoldingData> holdings = prepareHoldingsData();
        System.out.println("準備補回 " + holdings.size() + " 筆資料");

        try {
            recoverData(holdings);
            System.out.println("資料恢復完成！");
        } catch (IOException e) {
            System.err.println("資料恢復失敗: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static List<HoldingData> prepareHoldingsData() {
        List<HoldingData> list = new ArrayList<>();

        // 從截圖資料整理 (日期: 2025/11/26)
        // 格式: 股票代號, 股票名稱, 權重(%), 股數

        // 第一張截圖 (從上往下)
        list.add(new HoldingData("2330", "台積電", new BigDecimal("9.4"), 2231000L));
        list.add(new HoldingData("3017", "奇鋐", new BigDecimal("7"), 1726000L));
        list.add(new HoldingData("6669", "緯穎", new BigDecimal("6.87"), 527000L));
        list.add(new HoldingData("2383", "台光電", new BigDecimal("6.72"), 1561000L));
        list.add(new HoldingData("2368", "金像電", new BigDecimal("6.67"), 3738000L));
        list.add(new HoldingData("3665", "貿聯-KY", new BigDecimal("6.02"), 1369848L));
        list.add(new HoldingData("2345", "智邦", new BigDecimal("5.52"), 1888000L));
        list.add(new HoldingData("2308", "台達電", new BigDecimal("5.42"), 1959000L));
        list.add(new HoldingData("6223", "旺矽", new BigDecimal("4.68"), 761000L));

        // 第二張截圖
        list.add(new HoldingData("6274", "台燿", new BigDecimal("4.2"), 3336000L));
        list.add(new HoldingData("3653", "健策", new BigDecimal("4.13"), 536000L));
        list.add(new HoldingData("2449", "京元電子", new BigDecimal("3.49"), 5619000L));
        list.add(new HoldingData("6805", "富世達", new BigDecimal("3.3"), 781000L));
        list.add(new HoldingData("2317", "鴻海", new BigDecimal("3.04"), 4575000L));
        list.add(new HoldingData("8210", "勤誠", new BigDecimal("2.79"), 1036000L));
        list.add(new HoldingData("3231", "緯創", new BigDecimal("1.65"), 3955000L));
        list.add(new HoldingData("2059", "川湖", new BigDecimal("1.37"), 138000L));
        list.add(new HoldingData("3661", "世芯-KY", new BigDecimal("1.27"), 140000L));

        // 第三張截圖
        list.add(new HoldingData("6510", "精測", new BigDecimal("1.26"), 242000L));
        list.add(new HoldingData("6139", "亞翔", new BigDecimal("1.22"), 930000L));
        list.add(new HoldingData("6191", "精成科", new BigDecimal("1.06"), 2828000L));
        list.add(new HoldingData("1303", "南亞", new BigDecimal("1.03"), 6900000L));
        list.add(new HoldingData("5536", "聖暉*", new BigDecimal("0.97"), 495000L));
        list.add(new HoldingData("6515", "穎崴", new BigDecimal("0.94"), 121000L));
        list.add(new HoldingData("3533", "嘉澤", new BigDecimal("0.88"), 245000L));
        list.add(new HoldingData("2327", "國巨*", new BigDecimal("0.77"), 1129000L));
        list.add(new HoldingData("8358", "金居", new BigDecimal("0.76"), 1184000L));

        // 第四張截圖
        list.add(new HoldingData("3515", "華擎", new BigDecimal("0.74"), 1001000L));
        list.add(new HoldingData("2354", "鴻準", new BigDecimal("0.72"), 3806000L));
        list.add(new HoldingData("4958", "臻鼎-KY", new BigDecimal("0.68"), 1656000L));
        list.add(new HoldingData("3715", "定穎投控", new BigDecimal("0.52"), 1511000L));
        list.add(new HoldingData("1560", "中砂", new BigDecimal("0.48"), 517000L));
        list.add(new HoldingData("3211", "順達", new BigDecimal("0.36"), 469000L));
        list.add(new HoldingData("3081", "聯亞", new BigDecimal("0.19"), 126000L));
        list.add(new HoldingData("5347", "世界", new BigDecimal("0.18"), 684000L));
        list.add(new HoldingData("1319", "東陽", new BigDecimal("0.16"), 573000L));

        // 第五張截圖 (權重較低的)
        list.add(new HoldingData("3044", "健鼎", new BigDecimal("0.08"), 93000L));
        list.add(new HoldingData("2454", "聯發科", new BigDecimal("0.08"), 21000L));
        list.add(new HoldingData("3217", "優群", new BigDecimal("0.08"), 172000L));
        list.add(new HoldingData("5274", "信驊", new BigDecimal("0.02"), 1000L));
        list.add(new HoldingData("3008", "大立光", new BigDecimal("0.01"), 1000L));

        // 權重 0 的股票
        list.add(new HoldingData("3711", "日月光投控", new BigDecimal("0"), 1000L));
        list.add(new HoldingData("3037", "欣興", new BigDecimal("0"), 1000L));
        list.add(new HoldingData("2357", "華碩", new BigDecimal("0"), 1000L));
        list.add(new HoldingData("3583", "辛耘", new BigDecimal("0"), 1000L));
        list.add(new HoldingData("2884", "玉山金", new BigDecimal("0"), 1010L));
        list.add(new HoldingData("2439", "美律", new BigDecimal("0"), 1000L));
        list.add(new HoldingData("3045", "台灣大", new BigDecimal("0"), 1000L));
        list.add(new HoldingData("8299", "群聯", new BigDecimal("0"), 1000L));
        list.add(new HoldingData("8996", "高力", new BigDecimal("0"), 1000L));

        return list;
    }

    private static void recoverData(List<HoldingData> holdings) throws IOException {
        Path filePath = Path.of(FILE_PATH);

        // 確保目錄存在
        Files.createDirectories(filePath.getParent());

        Workbook workbook;

        // 載入或建立工作簿
        if (Files.exists(filePath)) {
            try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
                workbook = new XSSFWorkbook(fis);
            }
        } else {
            workbook = new XSSFWorkbook();
        }

        Sheet sheet = workbook.getSheet(SHEET_NAME);
        if (sheet == null) {
            sheet = workbook.createSheet(SHEET_NAME);
            // 建立標題行
            Row headerRow = sheet.createRow(0);
            String[] headers = { "日期", "股票代號", "股票名稱", "股數", "權重(%)" };
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
        }

        // 先刪除該日期的現有資料
        deleteExistingData(sheet, TARGET_DATE);

        // 取得最後一行
        int lastRowNum = sheet.getLastRowNum();
        int currentRow = lastRowNum + 1;

        // 寫入資料
        for (HoldingData holding : holdings) {
            Row row = sheet.createRow(currentRow++);
            row.createCell(0).setCellValue(TARGET_DATE);
            row.createCell(1).setCellValue(holding.stockCode);
            row.createCell(2).setCellValue(holding.stockName);
            row.createCell(3).setCellValue(holding.shares);
            row.createCell(4).setCellValue(holding.weight.doubleValue());
        }

        // 儲存檔案
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            workbook.write(fos);
        }

        workbook.close();
        System.out.println("已寫入 " + holdings.size() + " 筆資料到 " + filePath);
    }

    private static void deleteExistingData(Sheet sheet, String targetDate) {
        List<Integer> rowsToDelete = new ArrayList<>();

        for (Row row : sheet) {
            if (row.getRowNum() == 0)
                continue; // 跳過標題

            Cell dateCell = row.getCell(0);
            if (dateCell != null) {
                String cellValue = getCellStringValue(dateCell);
                if (targetDate.equals(cellValue)) {
                    rowsToDelete.add(row.getRowNum());
                }
            }
        }

        // 從後往前刪除以避免索引問題
        for (int i = rowsToDelete.size() - 1; i >= 0; i--) {
            int rowIndex = rowsToDelete.get(i);
            Row row = sheet.getRow(rowIndex);
            if (row != null) {
                sheet.removeRow(row);
                // 將後面的行往上移
                if (rowIndex < sheet.getLastRowNum()) {
                    sheet.shiftRows(rowIndex + 1, sheet.getLastRowNum(), -1);
                }
            }
        }

        if (!rowsToDelete.isEmpty()) {
            System.out.println("已刪除 " + rowsToDelete.size() + " 筆日期為 " + targetDate + " 的舊資料");
        }
    }

    private static String getCellStringValue(Cell cell) {
        if (cell == null)
            return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    java.time.LocalDate date = cell.getLocalDateTimeCellValue().toLocalDate();
                    return date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                }
                return String.valueOf((long) cell.getNumericCellValue());
            default:
                return "";
        }
    }

    static class HoldingData {
        String stockCode;
        String stockName;
        BigDecimal weight;
        Long shares;

        HoldingData(String stockCode, String stockName, BigDecimal weight, Long shares) {
            this.stockCode = stockCode;
            this.stockName = stockName;
            this.weight = weight;
            this.shares = shares;
        }
    }

    @Test
    public void recoverData20251127Test() {
        System.out.println("開始恢復 2025-11-27 資料...");

        List<HoldingData> holdings = prepareHoldingsData20251127();
        System.out.println("準備補回 " + holdings.size() + " 筆資料");

        try {
            recoverDataForDate(holdings, "2025-11-27");
            System.out.println("2025-11-27 資料恢復完成！");
        } catch (IOException e) {
            System.err.println("資料恢復失敗: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static List<HoldingData> prepareHoldingsData20251127() {
        List<HoldingData> list = new ArrayList<>();

        // 2025/11/27 資料 (從截圖整理)
        list.add(new HoldingData("2330", "台積電", new BigDecimal("9.18"), 2231000L));
        list.add(new HoldingData("3017", "奇鋐", new BigDecimal("6.9"), 1726000L));
        list.add(new HoldingData("2368", "金像電", new BigDecimal("6.72"), 3738000L));
        list.add(new HoldingData("6669", "緯穎", new BigDecimal("6.72"), 527000L));
        list.add(new HoldingData("2383", "台光電", new BigDecimal("6.69"), 1561000L));
        list.add(new HoldingData("3665", "貿聯-KY", new BigDecimal("6.19"), 1400848L));
        list.add(new HoldingData("2345", "智邦", new BigDecimal("5.58"), 1888000L));
        list.add(new HoldingData("2308", "台達電", new BigDecimal("5.29"), 1959000L));
        list.add(new HoldingData("6223", "旺矽", new BigDecimal("4.68"), 761000L));
        list.add(new HoldingData("3653", "健策", new BigDecimal("4.17"), 536000L));
        list.add(new HoldingData("6274", "台燿", new BigDecimal("4.13"), 3336000L));
        list.add(new HoldingData("2449", "京元電子", new BigDecimal("3.58"), 5619000L));
        list.add(new HoldingData("6805", "富世達", new BigDecimal("3.49"), 781000L));
        list.add(new HoldingData("2317", "鴻海", new BigDecimal("3.03"), 4575000L));
        list.add(new HoldingData("8210", "勤誠", new BigDecimal("2.87"), 1036000L));
        list.add(new HoldingData("3231", "緯創", new BigDecimal("1.65"), 3955000L));
        list.add(new HoldingData("2059", "川湖", new BigDecimal("1.63"), 158000L));
        list.add(new HoldingData("3661", "世芯-KY", new BigDecimal("1.32"), 140000L));
        list.add(new HoldingData("6510", "精測", new BigDecimal("1.25"), 242000L));
        list.add(new HoldingData("6139", "亞翔", new BigDecimal("1.19"), 930000L));
        list.add(new HoldingData("1303", "南亞", new BigDecimal("1.11"), 6900000L));
        list.add(new HoldingData("6191", "精成科", new BigDecimal("1.04"), 2828000L));
        list.add(new HoldingData("5536", "聖暉*", new BigDecimal("0.96"), 495000L));
        list.add(new HoldingData("6515", "穎崴", new BigDecimal("0.95"), 121000L));
        list.add(new HoldingData("3533", "嘉澤", new BigDecimal("0.92"), 245000L));
        list.add(new HoldingData("8358", "金居", new BigDecimal("0.77"), 1184000L));
        list.add(new HoldingData("3515", "華擎", new BigDecimal("0.72"), 1001000L));
        list.add(new HoldingData("2354", "鴻準", new BigDecimal("0.71"), 3806000L));
        list.add(new HoldingData("4958", "臻鼎-KY", new BigDecimal("0.68"), 1656000L));
        list.add(new HoldingData("3715", "定穎投控", new BigDecimal("0.51"), 1511000L));
        list.add(new HoldingData("3081", "聯亞", new BigDecimal("0.51"), 331000L));
        list.add(new HoldingData("1560", "中砂", new BigDecimal("0.47"), 517000L));
        list.add(new HoldingData("3211", "順達", new BigDecimal("0.36"), 469000L));
        list.add(new HoldingData("2327", "國巨*", new BigDecimal("0.33"), 488000L));
        list.add(new HoldingData("3711", "日月光投控", new BigDecimal("0.19"), 286000L));
        list.add(new HoldingData("5347", "世界", new BigDecimal("0.18"), 684000L));
        list.add(new HoldingData("1319", "東陽", new BigDecimal("0.16"), 573000L));
        list.add(new HoldingData("2454", "聯發科", new BigDecimal("0.08"), 21000L));
        list.add(new HoldingData("3217", "優群", new BigDecimal("0.08"), 172000L));
        list.add(new HoldingData("3044", "健鼎", new BigDecimal("0.08"), 93000L));
        list.add(new HoldingData("5274", "信驊", new BigDecimal("0.02"), 1000L));
        list.add(new HoldingData("3008", "大立光", new BigDecimal("0.01"), 1000L));
        list.add(new HoldingData("3045", "台灣大", new BigDecimal("0"), 1000L));
        list.add(new HoldingData("3583", "辛耘", new BigDecimal("0"), 1000L));
        list.add(new HoldingData("2884", "玉山金", new BigDecimal("0"), 1010L));
        list.add(new HoldingData("3037", "欣興", new BigDecimal("0"), 1000L));
        list.add(new HoldingData("2439", "美律", new BigDecimal("0"), 1000L));
        list.add(new HoldingData("2357", "華碩", new BigDecimal("0"), 1000L));
        list.add(new HoldingData("8996", "高力", new BigDecimal("0"), 1000L));
        list.add(new HoldingData("8299", "群聯", new BigDecimal("0"), 1000L));

        return list;
    }

    private static void recoverDataForDate(List<HoldingData> holdings, String targetDate) throws IOException {
        Path filePath = Path.of(FILE_PATH);

        Files.createDirectories(filePath.getParent());

        Workbook workbook;

        if (Files.exists(filePath)) {
            try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
                workbook = new XSSFWorkbook(fis);
            }
        } else {
            workbook = new XSSFWorkbook();
        }

        Sheet sheet = workbook.getSheet(SHEET_NAME);
        if (sheet == null) {
            sheet = workbook.createSheet(SHEET_NAME);
            Row headerRow = sheet.createRow(0);
            String[] headers = { "日期", "股票代號", "股票名稱", "股數", "權重(%)" };
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
        }

        deleteExistingData(sheet, targetDate);

        int lastRowNum = sheet.getLastRowNum();
        int currentRow = lastRowNum + 1;

        for (HoldingData holding : holdings) {
            Row row = sheet.createRow(currentRow++);
            row.createCell(0).setCellValue(targetDate);
            row.createCell(1).setCellValue(holding.stockCode);
            row.createCell(2).setCellValue(holding.stockName);
            row.createCell(3).setCellValue(holding.shares);
            row.createCell(4).setCellValue(holding.weight.doubleValue());
        }

        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            workbook.write(fos);
        }

        workbook.close();
        System.out.println("已寫入 " + holdings.size() + " 筆資料到 " + filePath);
    }
}
