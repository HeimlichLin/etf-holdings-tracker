package com.etf.tracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.etf.tracker.config.AppConfig;
import com.etf.tracker.model.DailySnapshot;
import com.etf.tracker.model.Holding;

/**
 * ExcelStorageService 單元測試
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
class ExcelStorageServiceTest {

    @TempDir
    Path tempDir;

    private ExcelStorageService storageService;
    private AppConfig appConfig;
    private Path testFilePath;

    @BeforeEach
    void setUp() {
        appConfig = createTestAppConfig();
        testFilePath = tempDir.resolve("test-holdings.xlsx");
        storageService = new ExcelStorageService(appConfig);
    }

    @Test
    @DisplayName("儲存快照到新 Excel 檔案")
    void saveSnapshot_NewFile_CreatesFile() {
        // Given
        DailySnapshot snapshot = createMockSnapshot(LocalDate.now());

        // When
        storageService.saveSnapshot(snapshot);

        // Then
        Path filePath = tempDir.resolve(appConfig.getData().getFileName());
        assertTrue(Files.exists(filePath));
    }

    @Test
    @DisplayName("儲存快照到已存在的 Excel 檔案")
    void saveSnapshot_ExistingFile_AppendsData() {
        // Given
        DailySnapshot snapshot1 = createMockSnapshot(LocalDate.now().minusDays(1));
        DailySnapshot snapshot2 = createMockSnapshot(LocalDate.now());

        // When
        storageService.saveSnapshot(snapshot1);
        storageService.saveSnapshot(snapshot2);

        // Then
        List<LocalDate> dates = storageService.getAvailableDates();
        assertEquals(2, dates.size());
    }

    @Test
    @DisplayName("讀取指定日期的快照")
    void getSnapshot_ExistingDate_ReturnsSnapshot() {
        // Given
        LocalDate targetDate = LocalDate.now();
        DailySnapshot snapshot = createMockSnapshot(targetDate);
        storageService.saveSnapshot(snapshot);

        // When
        Optional<DailySnapshot> result = storageService.getSnapshot(targetDate);

        // Then
        assertTrue(result.isPresent());
        assertEquals(targetDate, result.get().getDate());
        assertEquals(2, result.get().getTotalCount());
    }

    @Test
    @DisplayName("讀取不存在日期應回傳空")
    void getSnapshot_NonExistingDate_ReturnsEmpty() {
        // Given
        LocalDate targetDate = LocalDate.now();
        storageService.saveSnapshot(createMockSnapshot(targetDate));

        // When
        Optional<DailySnapshot> result = storageService.getSnapshot(targetDate.minusDays(10));

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("取得可用日期清單")
    void getAvailableDates_ReturnsAllDates() {
        // Given
        LocalDate date1 = LocalDate.now();
        LocalDate date2 = LocalDate.now().minusDays(1);
        LocalDate date3 = LocalDate.now().minusDays(2);

        storageService.saveSnapshot(createMockSnapshot(date2));
        storageService.saveSnapshot(createMockSnapshot(date1));
        storageService.saveSnapshot(createMockSnapshot(date3));

        // When
        List<LocalDate> dates = storageService.getAvailableDates();

        // Then
        assertEquals(3, dates.size());
        // 應該按降序排列
        assertEquals(date1, dates.get(0));
        assertEquals(date2, dates.get(1));
        assertEquals(date3, dates.get(2));
    }

    @Test
    @DisplayName("取得最新快照")
    void getLatestSnapshot_ReturnsNewest() {
        // Given
        LocalDate oldDate = LocalDate.now().minusDays(5);
        LocalDate newDate = LocalDate.now();

        storageService.saveSnapshot(createMockSnapshot(oldDate));
        storageService.saveSnapshot(createMockSnapshot(newDate));

        // When
        Optional<DailySnapshot> result = storageService.getLatestSnapshot();

        // Then
        assertTrue(result.isPresent());
        assertEquals(newDate, result.get().getDate());
    }

    @Test
    @DisplayName("空檔案取得最新快照應回傳空")
    void getLatestSnapshot_EmptyFile_ReturnsEmpty() {
        // When
        Optional<DailySnapshot> result = storageService.getLatestSnapshot();

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("覆寫已存在日期的資料")
    void saveSnapshot_SameDate_OverwritesData() {
        // Given
        LocalDate targetDate = LocalDate.now();
        DailySnapshot originalSnapshot = createMockSnapshot(targetDate);
        storageService.saveSnapshot(originalSnapshot);

        // 建立新快照，股數不同
        Holding newHolding = Holding.builder()
                .stockCode("2330")
                .stockName("台積電")
                .shares(9999999L)
                .weight(new BigDecimal("50.0000"))
                .build();

        DailySnapshot updatedSnapshot = DailySnapshot.builder()
                .date(targetDate)
                .holdings(List.of(newHolding))
                .totalCount(1)
                .totalWeight(new BigDecimal("50.0000"))
                .build();

        // When
        storageService.saveSnapshot(updatedSnapshot);

        // Then
        Optional<DailySnapshot> result = storageService.getSnapshot(targetDate);
        assertTrue(result.isPresent());
        assertEquals(1, result.get().getTotalCount());
        assertEquals(9999999L, result.get().getHoldings().get(0).getShares());
    }

    @Test
    @DisplayName("刪除過期資料")
    void deleteOldData_RemovesDataBeforeCutoff() {
        // Given
        LocalDate today = LocalDate.now();
        storageService.saveSnapshot(createMockSnapshot(today));
        storageService.saveSnapshot(createMockSnapshot(today.minusDays(30)));
        storageService.saveSnapshot(createMockSnapshot(today.minusDays(100))); // 過期

        // When - deleteDataBefore 返回的是刪除的行數，不是日期數
        // 每個快照有 2 個持倉，所以刪除 1 個過期日期會刪除 2 行
        int deletedCount = storageService.deleteDataBefore(today.minusDays(90));

        // Then
        assertEquals(2, deletedCount); // 2 行（1 個日期 x 2 個持倉）
        List<LocalDate> remainingDates = storageService.getAvailableDates();
        assertEquals(2, remainingDates.size());
    }

    @Test
    @DisplayName("儲存空快照應拋出例外")
    void saveSnapshot_NullSnapshot_ThrowsException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> storageService.saveSnapshot(null));
    }

    @Test
    @DisplayName("儲存無日期快照應拋出例外")
    void saveSnapshot_NullDate_ThrowsException() {
        // Given
        DailySnapshot snapshot = DailySnapshot.builder()
                .date(null)
                .holdings(List.of())
                .build();

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> storageService.saveSnapshot(snapshot));
    }

    private DailySnapshot createMockSnapshot(LocalDate date) {
        Holding holding1 = Holding.builder()
                .stockCode("2330")
                .stockName("台積電")
                .shares(1234567L)
                .weight(new BigDecimal("12.3456"))
                .build();

        Holding holding2 = Holding.builder()
                .stockCode("2317")
                .stockName("鴻海")
                .shares(987654L)
                .weight(new BigDecimal("8.7654"))
                .build();

        return DailySnapshot.builder()
                .date(date)
                .holdings(List.of(holding1, holding2))
                .totalCount(2)
                .totalWeight(new BigDecimal("21.1110"))
                .build();
    }

    private AppConfig createTestAppConfig() {
        AppConfig config = new AppConfig();

        AppConfig.DataConfig dataConfig = new AppConfig.DataConfig();
        dataConfig.setStoragePath(tempDir.toString());
        dataConfig.setFileName("test-holdings.xlsx");
        dataConfig.setRetentionDays(90);
        config.setData(dataConfig);

        return config;
    }
}
