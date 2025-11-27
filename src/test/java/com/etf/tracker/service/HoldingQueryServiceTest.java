package com.etf.tracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.etf.tracker.model.DailySnapshot;
import com.etf.tracker.model.Holding;

/**
 * HoldingQueryService 單元測試
 * <p>
 * 測試持倉查詢服務的各項功能
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HoldingQueryService 單元測試")
class HoldingQueryServiceTest {

    @Mock
    private ExcelStorageService excelStorageService;

    @InjectMocks
    private HoldingQueryService holdingQueryService;

    private DailySnapshot testSnapshot;
    private List<Holding> testHoldings;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2024, 1, 15);

        testHoldings = List.of(
                Holding.builder()
                        .stockCode("2330")
                        .stockName("台積電")
                        .shares(1000000L)
                        .weight(new BigDecimal("25.50"))
                        .build(),
                Holding.builder()
                        .stockCode("2454")
                        .stockName("聯發科")
                        .shares(500000L)
                        .weight(new BigDecimal("15.30"))
                        .build(),
                Holding.builder()
                        .stockCode("2317")
                        .stockName("鴻海")
                        .shares(300000L)
                        .weight(new BigDecimal("10.20"))
                        .build());

        testSnapshot = DailySnapshot.builder()
                .date(testDate)
                .holdings(testHoldings)
                .totalCount(3)
                .totalWeight(new BigDecimal("51.00"))
                .build();
    }

    @Nested
    @DisplayName("查詢指定日期持倉")
    class GetSnapshotByDateTest {

        @Test
        @DisplayName("查詢存在的日期 - 應回傳快照")
        void getByDate_WhenDataExists_ShouldReturnSnapshot() {
            when(excelStorageService.getSnapshot(testDate)).thenReturn(Optional.of(testSnapshot));

            Optional<DailySnapshot> result = holdingQueryService.getSnapshotByDate(testDate);

            assertThat(result).isPresent();
            assertThat(result.get().getDate()).isEqualTo(testDate);
            assertThat(result.get().getTotalCount()).isEqualTo(3);
            verify(excelStorageService).getSnapshot(testDate);
        }

        @Test
        @DisplayName("查詢不存在的日期 - 應回傳空")
        void getByDate_WhenDataNotExists_ShouldReturnEmpty() {
            LocalDate nonExistDate = LocalDate.of(2024, 12, 31);
            when(excelStorageService.getSnapshot(nonExistDate)).thenReturn(Optional.empty());

            Optional<DailySnapshot> result = holdingQueryService.getSnapshotByDate(nonExistDate);

            assertThat(result).isEmpty();
            verify(excelStorageService).getSnapshot(nonExistDate);
        }

        @Test
        @DisplayName("查詢 null 日期 - 應回傳空")
        void getByDate_WhenDateIsNull_ShouldReturnEmpty() {
            Optional<DailySnapshot> result = holdingQueryService.getSnapshotByDate(null);

            assertThat(result).isEmpty();
            verify(excelStorageService, never()).getSnapshot(any());
        }
    }

    @Nested
    @DisplayName("查詢最新持倉")
    class GetLatestSnapshotTest {

        @Test
        @DisplayName("有資料時 - 應回傳最新快照")
        void getLatest_WhenDataExists_ShouldReturnLatestSnapshot() {
            when(excelStorageService.getLatestSnapshot()).thenReturn(Optional.of(testSnapshot));

            Optional<DailySnapshot> result = holdingQueryService.getLatestSnapshot();

            assertThat(result).isPresent();
            assertThat(result.get().getDate()).isEqualTo(testDate);
            verify(excelStorageService).getLatestSnapshot();
        }

        @Test
        @DisplayName("無資料時 - 應回傳空")
        void getLatest_WhenNoData_ShouldReturnEmpty() {
            when(excelStorageService.getLatestSnapshot()).thenReturn(Optional.empty());

            Optional<DailySnapshot> result = holdingQueryService.getLatestSnapshot();

            assertThat(result).isEmpty();
            verify(excelStorageService).getLatestSnapshot();
        }
    }

    @Nested
    @DisplayName("查詢可用日期")
    class GetAvailableDatesTest {

        @Test
        @DisplayName("有資料時 - 應回傳日期清單（降序）")
        void getAvailableDates_WhenDataExists_ShouldReturnDatesDescending() {
            List<LocalDate> dates = List.of(
                    LocalDate.of(2024, 1, 15),
                    LocalDate.of(2024, 1, 14),
                    LocalDate.of(2024, 1, 13));
            when(excelStorageService.getAvailableDates()).thenReturn(dates);

            List<LocalDate> result = holdingQueryService.getAvailableDates();

            assertThat(result).hasSize(3);
            assertThat(result.get(0)).isEqualTo(LocalDate.of(2024, 1, 15));
            assertThat(result.get(2)).isEqualTo(LocalDate.of(2024, 1, 13));
            verify(excelStorageService).getAvailableDates();
        }

        @Test
        @DisplayName("無資料時 - 應回傳空清單")
        void getAvailableDates_WhenNoData_ShouldReturnEmptyList() {
            when(excelStorageService.getAvailableDates()).thenReturn(Collections.emptyList());

            List<LocalDate> result = holdingQueryService.getAvailableDates();

            assertThat(result).isEmpty();
            verify(excelStorageService).getAvailableDates();
        }
    }

    @Nested
    @DisplayName("依股票代號搜尋")
    class SearchByStockCodeTest {

        @Test
        @DisplayName("搜尋存在的股票代號 - 應回傳符合的持倉")
        void searchByCode_WhenExists_ShouldReturnMatchingHoldings() {
            when(excelStorageService.getLatestSnapshot()).thenReturn(Optional.of(testSnapshot));

            List<Holding> result = holdingQueryService.searchByStockCode("2330");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStockCode()).isEqualTo("2330");
            assertThat(result.get(0).getStockName()).isEqualTo("台積電");
        }

        @Test
        @DisplayName("搜尋部分符合的股票代號 - 應回傳符合的持倉")
        void searchByCode_WhenPartialMatch_ShouldReturnMatchingHoldings() {
            when(excelStorageService.getLatestSnapshot()).thenReturn(Optional.of(testSnapshot));

            List<Holding> result = holdingQueryService.searchByStockCode("23");

            assertThat(result).hasSize(2); // 2330, 2317
        }

        @Test
        @DisplayName("搜尋不存在的股票代號 - 應回傳空清單")
        void searchByCode_WhenNotExists_ShouldReturnEmptyList() {
            when(excelStorageService.getLatestSnapshot()).thenReturn(Optional.of(testSnapshot));

            List<Holding> result = holdingQueryService.searchByStockCode("9999");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("無資料時搜尋 - 應回傳空清單")
        void searchByCode_WhenNoData_ShouldReturnEmptyList() {
            when(excelStorageService.getLatestSnapshot()).thenReturn(Optional.empty());

            List<Holding> result = holdingQueryService.searchByStockCode("2330");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("依股票名稱搜尋")
    class SearchByStockNameTest {

        @Test
        @DisplayName("搜尋存在的股票名稱 - 應回傳符合的持倉")
        void searchByName_WhenExists_ShouldReturnMatchingHoldings() {
            when(excelStorageService.getLatestSnapshot()).thenReturn(Optional.of(testSnapshot));

            List<Holding> result = holdingQueryService.searchByStockName("台積電");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStockName()).isEqualTo("台積電");
        }

        @Test
        @DisplayName("搜尋部分符合的股票名稱 - 應回傳符合的持倉")
        void searchByName_WhenPartialMatch_ShouldReturnMatchingHoldings() {
            when(excelStorageService.getLatestSnapshot()).thenReturn(Optional.of(testSnapshot));

            List<Holding> result = holdingQueryService.searchByStockName("電");

            assertThat(result).hasSize(1); // 台積電
        }

        @Test
        @DisplayName("大小寫不敏感搜尋")
        void searchByName_ShouldBeCaseInsensitive() {
            when(excelStorageService.getLatestSnapshot()).thenReturn(Optional.of(testSnapshot));

            List<Holding> result = holdingQueryService.searchByStockName("聯發科");

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("通用搜尋（代號或名稱）")
    class SearchTest {

        @Test
        @DisplayName("搜尋股票代號 - 應回傳符合的持倉")
        void search_ByCode_ShouldReturnMatchingHoldings() {
            when(excelStorageService.getLatestSnapshot()).thenReturn(Optional.of(testSnapshot));

            List<Holding> result = holdingQueryService.search("2454");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStockName()).isEqualTo("聯發科");
        }

        @Test
        @DisplayName("搜尋股票名稱 - 應回傳符合的持倉")
        void search_ByName_ShouldReturnMatchingHoldings() {
            when(excelStorageService.getLatestSnapshot()).thenReturn(Optional.of(testSnapshot));

            List<Holding> result = holdingQueryService.search("鴻海");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStockCode()).isEqualTo("2317");
        }

        @Test
        @DisplayName("空查詢 - 應回傳所有持倉")
        void search_WithEmptyKeyword_ShouldReturnAllHoldings() {
            when(excelStorageService.getLatestSnapshot()).thenReturn(Optional.of(testSnapshot));

            List<Holding> result = holdingQueryService.search("");

            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("null 查詢 - 應回傳所有持倉")
        void search_WithNullKeyword_ShouldReturnAllHoldings() {
            when(excelStorageService.getLatestSnapshot()).thenReturn(Optional.of(testSnapshot));

            List<Holding> result = holdingQueryService.search(null);

            assertThat(result).hasSize(3);
        }
    }

    @Nested
    @DisplayName("統計資訊")
    class StatisticsTest {

        @Test
        @DisplayName("取得持倉統計 - 應回傳正確的統計數據")
        void getStatistics_ShouldReturnCorrectStats() {
            when(excelStorageService.getLatestSnapshot()).thenReturn(Optional.of(testSnapshot));

            var stats = holdingQueryService.getStatistics();

            assertThat(stats).isNotNull();
            assertThat(stats.totalCount()).isEqualTo(3);
            assertThat(stats.totalWeight()).isEqualByComparingTo(new BigDecimal("51.00"));
            assertThat(stats.date()).isEqualTo(testDate);
        }

        @Test
        @DisplayName("無資料時取得統計 - 應回傳空統計")
        void getStatistics_WhenNoData_ShouldReturnEmptyStats() {
            when(excelStorageService.getLatestSnapshot()).thenReturn(Optional.empty());

            var stats = holdingQueryService.getStatistics();

            assertThat(stats).isNotNull();
            assertThat(stats.totalCount()).isEqualTo(0);
            assertThat(stats.totalWeight()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(stats.date()).isNull();
        }
    }

    @Nested
    @DisplayName("分頁查詢")
    class PaginationTest {

        @Test
        @DisplayName("取得第一頁資料")
        void getPage_FirstPage_ShouldReturnCorrectData() {
            when(excelStorageService.getLatestSnapshot()).thenReturn(Optional.of(testSnapshot));

            List<Holding> result = holdingQueryService.getHoldingsPage(0, 2);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("取得最後一頁資料")
        void getPage_LastPage_ShouldReturnRemainingData() {
            when(excelStorageService.getLatestSnapshot()).thenReturn(Optional.of(testSnapshot));

            List<Holding> result = holdingQueryService.getHoldingsPage(1, 2);

            assertThat(result).hasSize(1); // 3 total, page size 2, page 1 = 1 item
        }

        @Test
        @DisplayName("超出範圍的頁碼 - 應回傳空清單")
        void getPage_OutOfRange_ShouldReturnEmptyList() {
            when(excelStorageService.getLatestSnapshot()).thenReturn(Optional.of(testSnapshot));

            List<Holding> result = holdingQueryService.getHoldingsPage(10, 2);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("排序功能")
    class SortingTest {

        @Test
        @DisplayName("依權重降序排序")
        void sortByWeight_Descending_ShouldReturnSortedList() {
            when(excelStorageService.getLatestSnapshot()).thenReturn(Optional.of(testSnapshot));

            List<Holding> result = holdingQueryService.getHoldingsSortedByWeight(false);

            assertThat(result).hasSize(3);
            assertThat(result.get(0).getWeight()).isEqualByComparingTo(new BigDecimal("25.50"));
            assertThat(result.get(2).getWeight()).isEqualByComparingTo(new BigDecimal("10.20"));
        }

        @Test
        @DisplayName("依權重升序排序")
        void sortByWeight_Ascending_ShouldReturnSortedList() {
            when(excelStorageService.getLatestSnapshot()).thenReturn(Optional.of(testSnapshot));

            List<Holding> result = holdingQueryService.getHoldingsSortedByWeight(true);

            assertThat(result).hasSize(3);
            assertThat(result.get(0).getWeight()).isEqualByComparingTo(new BigDecimal("10.20"));
            assertThat(result.get(2).getWeight()).isEqualByComparingTo(new BigDecimal("25.50"));
        }

        @Test
        @DisplayName("依股票代號排序")
        void sortByStockCode_ShouldReturnSortedList() {
            when(excelStorageService.getLatestSnapshot()).thenReturn(Optional.of(testSnapshot));

            List<Holding> result = holdingQueryService.getHoldingsSortedByCode(true);

            assertThat(result).hasSize(3);
            assertThat(result.get(0).getStockCode()).isEqualTo("2317");
            assertThat(result.get(2).getStockCode()).isEqualTo("2454");
        }
    }
}
