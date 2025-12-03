package com.etf.tracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.etf.tracker.config.AppConfig;
import com.etf.tracker.dto.CleanupResultDto;

/**
 * DataCleanupService 單元測試
 * <p>
 * 測試 90 天資料清理功能
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DataCleanupService 單元測試")
class DataCleanupServiceTest {

    @Mock
    private StorageService storageService;

    @Mock
    private AppConfig appConfig;

    @Mock
    private AppConfig.DataConfig dataConfig;

    private DataCleanupService dataCleanupService;

    @BeforeEach
    void setUp() {
        when(appConfig.getData()).thenReturn(dataConfig);
        when(dataConfig.getRetentionDays()).thenReturn(90);
        dataCleanupService = new DataCleanupService(storageService, appConfig);
    }

    @Nested
    @DisplayName("cleanupOldData 方法測試")
    class CleanupOldDataTest {

        @Test
        @DisplayName("當有過期資料時，應成功刪除並回傳正確結果")
        void shouldDeleteExpiredDataSuccessfully() {
            // Arrange
            LocalDate cutoffDate = LocalDate.now().minusDays(90);
            when(storageService.deleteDataBefore(any(LocalDate.class))).thenReturn(5);
            when(storageService.getAvailableDates()).thenReturn(
                    Arrays.asList(
                            LocalDate.now(),
                            LocalDate.now().minusDays(30),
                            LocalDate.now().minusDays(60)));

            // Act
            CleanupResultDto result = dataCleanupService.cleanupOldData();

            // Assert
            assertThat(result.success()).isTrue();
            assertThat(result.deletedRecords()).isEqualTo(5);
            assertThat(result.remainingRecords()).isEqualTo(3);
            assertThat(result.remainingDays()).isEqualTo(90);
            assertThat(result.cutoffDate()).isEqualTo(cutoffDate);
            assertThat(result.message()).contains("5");
        }

        @Test
        @DisplayName("當沒有過期資料時，應回傳零刪除記錄")
        void shouldReturnZeroWhenNoExpiredData() {
            // Arrange
            when(storageService.deleteDataBefore(any(LocalDate.class))).thenReturn(0);
            when(storageService.getAvailableDates()).thenReturn(
                    Arrays.asList(LocalDate.now(), LocalDate.now().minusDays(30)));

            // Act
            CleanupResultDto result = dataCleanupService.cleanupOldData();

            // Assert
            assertThat(result.success()).isTrue();
            assertThat(result.deletedRecords()).isEqualTo(0);
            assertThat(result.remainingRecords()).isEqualTo(2);
            assertThat(result.hasDeletedData()).isFalse();
        }

        @Test
        @DisplayName("當沒有任何資料時，應正確處理")
        void shouldHandleEmptyDataCorrectly() {
            // Arrange
            when(storageService.deleteDataBefore(any(LocalDate.class))).thenReturn(0);
            when(storageService.getAvailableDates()).thenReturn(Collections.emptyList());

            // Act
            CleanupResultDto result = dataCleanupService.cleanupOldData();

            // Assert
            assertThat(result.success()).isTrue();
            assertThat(result.deletedRecords()).isEqualTo(0);
            assertThat(result.remainingRecords()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("cleanupOldData 帶參數方法測試")
    class CleanupOldDataWithDaysTest {

        @Test
        @DisplayName("應使用指定的保留天數進行清理")
        void shouldUseSpecifiedRetentionDays() {
            // Arrange
            int customDays = 30;
            LocalDate expectedCutoffDate = LocalDate.now().minusDays(customDays);
            when(storageService.deleteDataBefore(expectedCutoffDate)).thenReturn(10);
            when(storageService.getAvailableDates()).thenReturn(
                    Arrays.asList(LocalDate.now(), LocalDate.now().minusDays(15)));

            // Act
            CleanupResultDto result = dataCleanupService.cleanupOldData(customDays);

            // Assert
            assertThat(result.success()).isTrue();
            assertThat(result.deletedRecords()).isEqualTo(10);
            assertThat(result.remainingDays()).isEqualTo(customDays);
            assertThat(result.cutoffDate()).isEqualTo(expectedCutoffDate);
            verify(storageService).deleteDataBefore(expectedCutoffDate);
        }

        @Test
        @DisplayName("當保留天數為零時，應清理所有資料")
        void shouldCleanAllDataWhenDaysIsZero() {
            // Arrange
            LocalDate today = LocalDate.now();
            when(storageService.deleteDataBefore(today)).thenReturn(100);
            when(storageService.getAvailableDates()).thenReturn(Collections.emptyList());

            // Act
            CleanupResultDto result = dataCleanupService.cleanupOldData(0);

            // Assert
            assertThat(result.success()).isTrue();
            assertThat(result.deletedRecords()).isEqualTo(100);
            assertThat(result.remainingRecords()).isEqualTo(0);
        }

        @Test
        @DisplayName("當保留天數為負數時，應拋出異常")
        void shouldThrowExceptionWhenDaysIsNegative() {
            // Act & Assert
            assertThatThrownBy(() -> dataCleanupService.cleanupOldData(-1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("保留天數不可為負數");
        }
    }

    @Nested
    @DisplayName("previewCleanup 方法測試")
    class PreviewCleanupTest {

        @Test
        @DisplayName("應正確預覽將被刪除的資料")
        void shouldPreviewDeletedDataCorrectly() {
            // Arrange
            LocalDate cutoffDate = LocalDate.now().minusDays(90);
            List<LocalDate> allDates = Arrays.asList(
                    LocalDate.now(),
                    LocalDate.now().minusDays(30),
                    LocalDate.now().minusDays(60),
                    LocalDate.now().minusDays(100),
                    LocalDate.now().minusDays(120));
            when(storageService.getAvailableDates()).thenReturn(allDates);

            // Act
            List<LocalDate> toBeDeleted = dataCleanupService.previewCleanup(90);

            // Assert
            assertThat(toBeDeleted).hasSize(2);
            assertThat(toBeDeleted).allMatch(date -> date.isBefore(cutoffDate));
        }

        @Test
        @DisplayName("當沒有過期資料時，預覽應回傳空清單")
        void shouldReturnEmptyListWhenNoExpiredData() {
            // Arrange
            List<LocalDate> recentDates = Arrays.asList(
                    LocalDate.now(),
                    LocalDate.now().minusDays(30));
            when(storageService.getAvailableDates()).thenReturn(recentDates);

            // Act
            List<LocalDate> toBeDeleted = dataCleanupService.previewCleanup(90);

            // Assert
            assertThat(toBeDeleted).isEmpty();
        }
    }

    @Nested
    @DisplayName("getCleanupStatistics 方法測試")
    class GetCleanupStatisticsTest {

        @Test
        @DisplayName("應正確計算清理統計資訊")
        void shouldCalculateStatisticsCorrectly() {
            // Arrange
            List<LocalDate> allDates = Arrays.asList(
                    LocalDate.now(),
                    LocalDate.now().minusDays(30),
                    LocalDate.now().minusDays(60),
                    LocalDate.now().minusDays(100));
            when(storageService.getAvailableDates()).thenReturn(allDates);

            // Act
            DataCleanupService.CleanupStatistics stats = dataCleanupService.getCleanupStatistics();

            // Assert
            assertThat(stats.totalRecords()).isEqualTo(4);
            assertThat(stats.expiredRecords()).isEqualTo(1);
            assertThat(stats.validRecords()).isEqualTo(3);
            assertThat(stats.oldestDate()).isEqualTo(LocalDate.now().minusDays(100));
            assertThat(stats.newestDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("當沒有資料時，統計應正確處理")
        void shouldHandleEmptyStatistics() {
            // Arrange
            when(storageService.getAvailableDates()).thenReturn(Collections.emptyList());

            // Act
            DataCleanupService.CleanupStatistics stats = dataCleanupService.getCleanupStatistics();

            // Assert
            assertThat(stats.totalRecords()).isEqualTo(0);
            assertThat(stats.expiredRecords()).isEqualTo(0);
            assertThat(stats.validRecords()).isEqualTo(0);
            assertThat(stats.oldestDate()).isNull();
            assertThat(stats.newestDate()).isNull();
        }
    }

    @Nested
    @DisplayName("confirmAndCleanup 方法測試")
    class ConfirmAndCleanupTest {

        @Test
        @DisplayName("當未確認時，應不執行清理")
        void shouldNotCleanupWhenNotConfirmed() {
            // Act
            CleanupResultDto result = dataCleanupService.confirmAndCleanup(90, false);

            // Assert
            assertThat(result.success()).isFalse();
            assertThat(result.message()).contains("確認");
            verify(storageService, never()).deleteDataBefore(any());
        }

        @Test
        @DisplayName("當確認時，應執行清理")
        void shouldCleanupWhenConfirmed() {
            // Arrange
            when(storageService.deleteDataBefore(any(LocalDate.class))).thenReturn(5);
            when(storageService.getAvailableDates()).thenReturn(Arrays.asList(LocalDate.now()));

            // Act
            CleanupResultDto result = dataCleanupService.confirmAndCleanup(90, true);

            // Assert
            assertThat(result.success()).isTrue();
            assertThat(result.deletedRecords()).isEqualTo(5);
            verify(storageService).deleteDataBefore(any(LocalDate.class));
        }
    }

    @Nested
    @DisplayName("getDefaultRetentionDays 方法測試")
    class GetDefaultRetentionDaysTest {

        @Test
        @DisplayName("應從配置中取得預設保留天數")
        void shouldGetDefaultRetentionDaysFromConfig() {
            // Act
            int days = dataCleanupService.getDefaultRetentionDays();

            // Assert
            assertThat(days).isEqualTo(90);
        }
    }
}
