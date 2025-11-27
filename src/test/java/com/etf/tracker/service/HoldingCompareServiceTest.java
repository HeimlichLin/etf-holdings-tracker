package com.etf.tracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
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

import com.etf.tracker.dto.RangeCompareResultDto;
import com.etf.tracker.exception.ValidationException;
import com.etf.tracker.model.ChangeType;
import com.etf.tracker.model.DailySnapshot;
import com.etf.tracker.model.Holding;

/**
 * HoldingCompareService 單元測試
 * <p>
 * 測試持倉比較服務的各項功能，包括：
 * <ul>
 * <li>新進增持識別</li>
 * <li>剔除減持識別</li>
 * <li>增持/減持計算</li>
 * <li>不變識別</li>
 * <li>變化比例計算</li>
 * <li>邊界條件處理</li>
 * </ul>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HoldingCompareService 單元測試")
class HoldingCompareServiceTest {

    @Mock
    private ExcelStorageService excelStorageService;

    @InjectMocks
    private HoldingCompareService holdingCompareService;

    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {
        startDate = LocalDate.of(2024, 1, 10);
        endDate = LocalDate.of(2024, 1, 15);
    }

    // ========== Helper Methods ==========

    private Holding createHolding(String code, String name, long shares, String weight) {
        return Holding.builder()
                .stockCode(code)
                .stockName(name)
                .shares(shares)
                .weight(new BigDecimal(weight))
                .build();
    }

    private DailySnapshot createSnapshot(LocalDate date, List<Holding> holdings) {
        BigDecimal totalWeight = holdings.stream()
                .map(Holding::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DailySnapshot.builder()
                .date(date)
                .holdings(holdings)
                .totalCount(holdings.size())
                .totalWeight(totalWeight)
                .build();
    }

    // ========== 日期驗證測試 ==========

    @Nested
    @DisplayName("日期驗證")
    class DateValidationTest {

        @Test
        @DisplayName("起始日期晚於結束日期 - 應拋出 ValidationException")
        void compare_WhenStartDateAfterEndDate_ShouldThrowException() {
            LocalDate invalidStartDate = LocalDate.of(2024, 1, 20);
            LocalDate invalidEndDate = LocalDate.of(2024, 1, 10);

            assertThatThrownBy(() -> holdingCompareService.compareHoldings(invalidStartDate, invalidEndDate))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("起始日期必須早於或等於結束日期");
        }

        @Test
        @DisplayName("起始日期等於結束日期 - 應允許比較")
        void compare_WhenStartDateEqualsEndDate_ShouldAllowComparison() {
            LocalDate sameDate = LocalDate.of(2024, 1, 15);
            List<Holding> holdings = List.of(
                    createHolding("2330", "台積電", 1000000L, "25.50"));
            DailySnapshot snapshot = createSnapshot(sameDate, holdings);

            when(excelStorageService.getSnapshot(sameDate)).thenReturn(Optional.of(snapshot));

            RangeCompareResultDto result = holdingCompareService.compareHoldings(sameDate, sameDate);

            assertThat(result).isNotNull();
            assertThat(result.startDate()).isEqualTo(sameDate);
            assertThat(result.endDate()).isEqualTo(sameDate);
            // 同日期比較，所有股票應為 UNCHANGED
            assertThat(result.unchanged()).hasSize(1);
            assertThat(result.newAdditions()).isEmpty();
            assertThat(result.removals()).isEmpty();
        }

        @Test
        @DisplayName("起始日期無資料 - 應拋出 ValidationException")
        void compare_WhenStartDateNotFound_ShouldThrowException() {
            when(excelStorageService.getSnapshot(startDate)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> holdingCompareService.compareHoldings(startDate, endDate))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("起始日期無資料");
        }

        @Test
        @DisplayName("結束日期無資料 - 應拋出 ValidationException")
        void compare_WhenEndDateNotFound_ShouldThrowException() {
            DailySnapshot startSnapshot = createSnapshot(startDate, List.of(
                    createHolding("2330", "台積電", 1000000L, "25.50")));

            when(excelStorageService.getSnapshot(startDate)).thenReturn(Optional.of(startSnapshot));
            when(excelStorageService.getSnapshot(endDate)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> holdingCompareService.compareHoldings(startDate, endDate))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("結束日期無資料");
        }
    }

    // ========== 新進增持測試 ==========

    @Nested
    @DisplayName("新進增持識別")
    class NewAdditionTest {

        @Test
        @DisplayName("識別新進股票 - 起始日不存在，結束日存在")
        void compare_WhenStockAddedInEndDate_ShouldIdentifyAsNewAddition() {
            List<Holding> startHoldings = List.of(
                    createHolding("2330", "台積電", 1000000L, "25.50"));
            List<Holding> endHoldings = List.of(
                    createHolding("2330", "台積電", 1000000L, "24.50"),
                    createHolding("2454", "聯發科", 500000L, "15.30"));

            DailySnapshot startSnapshot = createSnapshot(startDate, startHoldings);
            DailySnapshot endSnapshot = createSnapshot(endDate, endHoldings);

            when(excelStorageService.getSnapshot(startDate)).thenReturn(Optional.of(startSnapshot));
            when(excelStorageService.getSnapshot(endDate)).thenReturn(Optional.of(endSnapshot));

            RangeCompareResultDto result = holdingCompareService.compareHoldings(startDate, endDate);

            assertThat(result.newAdditions()).hasSize(1);
            assertThat(result.newAdditions().get(0).stockCode()).isEqualTo("2454");
            assertThat(result.newAdditions().get(0).stockName()).isEqualTo("聯發科");
            assertThat(result.newAdditions().get(0).changeType()).isEqualTo(ChangeType.NEW_ADDITION);
            assertThat(result.newAdditions().get(0).startShares()).isNull();
            assertThat(result.newAdditions().get(0).endShares()).isEqualTo(500000L);
            assertThat(result.newAdditions().get(0).sharesDiff()).isEqualTo(500000L);
            assertThat(result.newAdditions().get(0).startWeight()).isNull();
            assertThat(result.newAdditions().get(0).endWeight()).isEqualByComparingTo(new BigDecimal("15.30"));
        }

        @Test
        @DisplayName("新進股票的變化比例為 null")
        void compare_WhenNewAddition_ChangeRatioShouldBeNull() {
            List<Holding> startHoldings = List.of();
            List<Holding> endHoldings = List.of(
                    createHolding("2454", "聯發科", 500000L, "15.30"));

            DailySnapshot startSnapshot = createSnapshot(startDate, startHoldings);
            DailySnapshot endSnapshot = createSnapshot(endDate, endHoldings);

            when(excelStorageService.getSnapshot(startDate)).thenReturn(Optional.of(startSnapshot));
            when(excelStorageService.getSnapshot(endDate)).thenReturn(Optional.of(endSnapshot));

            RangeCompareResultDto result = holdingCompareService.compareHoldings(startDate, endDate);

            assertThat(result.newAdditions()).hasSize(1);
            assertThat(result.newAdditions().get(0).changeRatio()).isNull();
        }
    }

    // ========== 剔除減持測試 ==========

    @Nested
    @DisplayName("剔除減持識別")
    class RemovalTest {

        @Test
        @DisplayName("識別剔除股票 - 起始日存在，結束日不存在")
        void compare_WhenStockRemovedInEndDate_ShouldIdentifyAsRemoval() {
            List<Holding> startHoldings = List.of(
                    createHolding("2330", "台積電", 1000000L, "25.50"),
                    createHolding("2412", "中華電", 300000L, "8.50"));
            List<Holding> endHoldings = List.of(
                    createHolding("2330", "台積電", 1000000L, "26.50"));

            DailySnapshot startSnapshot = createSnapshot(startDate, startHoldings);
            DailySnapshot endSnapshot = createSnapshot(endDate, endHoldings);

            when(excelStorageService.getSnapshot(startDate)).thenReturn(Optional.of(startSnapshot));
            when(excelStorageService.getSnapshot(endDate)).thenReturn(Optional.of(endSnapshot));

            RangeCompareResultDto result = holdingCompareService.compareHoldings(startDate, endDate);

            assertThat(result.removals()).hasSize(1);
            assertThat(result.removals().get(0).stockCode()).isEqualTo("2412");
            assertThat(result.removals().get(0).stockName()).isEqualTo("中華電");
            assertThat(result.removals().get(0).changeType()).isEqualTo(ChangeType.REMOVED);
            assertThat(result.removals().get(0).startShares()).isEqualTo(300000L);
            assertThat(result.removals().get(0).endShares()).isNull();
            assertThat(result.removals().get(0).sharesDiff()).isEqualTo(-300000L);
            assertThat(result.removals().get(0).startWeight()).isEqualByComparingTo(new BigDecimal("8.50"));
            assertThat(result.removals().get(0).endWeight()).isNull();
        }

        @Test
        @DisplayName("剔除股票的變化比例為 -100%")
        void compare_WhenRemoval_ChangeRatioShouldBeNegative100() {
            List<Holding> startHoldings = List.of(
                    createHolding("2412", "中華電", 300000L, "8.50"));
            List<Holding> endHoldings = List.of();

            DailySnapshot startSnapshot = createSnapshot(startDate, startHoldings);
            DailySnapshot endSnapshot = createSnapshot(endDate, endHoldings);

            when(excelStorageService.getSnapshot(startDate)).thenReturn(Optional.of(startSnapshot));
            when(excelStorageService.getSnapshot(endDate)).thenReturn(Optional.of(endSnapshot));

            RangeCompareResultDto result = holdingCompareService.compareHoldings(startDate, endDate);

            assertThat(result.removals()).hasSize(1);
            assertThat(result.removals().get(0).changeRatio())
                    .isEqualByComparingTo(new BigDecimal("-100.00"));
        }
    }

    // ========== 增持測試 ==========

    @Nested
    @DisplayName("增持識別")
    class IncreasedTest {

        @Test
        @DisplayName("識別增持股票 - 股數增加")
        void compare_WhenSharesIncreased_ShouldIdentifyAsIncreased() {
            List<Holding> startHoldings = List.of(
                    createHolding("2330", "台積電", 1000000L, "25.00"));
            List<Holding> endHoldings = List.of(
                    createHolding("2330", "台積電", 1200000L, "28.00"));

            DailySnapshot startSnapshot = createSnapshot(startDate, startHoldings);
            DailySnapshot endSnapshot = createSnapshot(endDate, endHoldings);

            when(excelStorageService.getSnapshot(startDate)).thenReturn(Optional.of(startSnapshot));
            when(excelStorageService.getSnapshot(endDate)).thenReturn(Optional.of(endSnapshot));

            RangeCompareResultDto result = holdingCompareService.compareHoldings(startDate, endDate);

            assertThat(result.increased()).hasSize(1);
            assertThat(result.increased().get(0).stockCode()).isEqualTo("2330");
            assertThat(result.increased().get(0).changeType()).isEqualTo(ChangeType.INCREASED);
            assertThat(result.increased().get(0).startShares()).isEqualTo(1000000L);
            assertThat(result.increased().get(0).endShares()).isEqualTo(1200000L);
            assertThat(result.increased().get(0).sharesDiff()).isEqualTo(200000L);
        }

        @Test
        @DisplayName("增持變化比例計算正確")
        void compare_WhenIncreased_ChangeRatioShouldBeCorrect() {
            List<Holding> startHoldings = List.of(
                    createHolding("2330", "台積電", 1000000L, "25.00"));
            List<Holding> endHoldings = List.of(
                    createHolding("2330", "台積電", 1200000L, "28.00"));

            DailySnapshot startSnapshot = createSnapshot(startDate, startHoldings);
            DailySnapshot endSnapshot = createSnapshot(endDate, endHoldings);

            when(excelStorageService.getSnapshot(startDate)).thenReturn(Optional.of(startSnapshot));
            when(excelStorageService.getSnapshot(endDate)).thenReturn(Optional.of(endSnapshot));

            RangeCompareResultDto result = holdingCompareService.compareHoldings(startDate, endDate);

            // 變化比例 = (1200000 - 1000000) / 1000000 * 100 = 20%
            assertThat(result.increased().get(0).changeRatio())
                    .isEqualByComparingTo(new BigDecimal("20.00"));
        }

        @Test
        @DisplayName("增持權重變化計算正確")
        void compare_WhenIncreased_WeightDiffShouldBeCorrect() {
            List<Holding> startHoldings = List.of(
                    createHolding("2330", "台積電", 1000000L, "25.00"));
            List<Holding> endHoldings = List.of(
                    createHolding("2330", "台積電", 1200000L, "28.50"));

            DailySnapshot startSnapshot = createSnapshot(startDate, startHoldings);
            DailySnapshot endSnapshot = createSnapshot(endDate, endHoldings);

            when(excelStorageService.getSnapshot(startDate)).thenReturn(Optional.of(startSnapshot));
            when(excelStorageService.getSnapshot(endDate)).thenReturn(Optional.of(endSnapshot));

            RangeCompareResultDto result = holdingCompareService.compareHoldings(startDate, endDate);

            assertThat(result.increased().get(0).startWeight())
                    .isEqualByComparingTo(new BigDecimal("25.00"));
            assertThat(result.increased().get(0).endWeight())
                    .isEqualByComparingTo(new BigDecimal("28.50"));
            assertThat(result.increased().get(0).weightDiff())
                    .isEqualByComparingTo(new BigDecimal("3.50"));
        }
    }

    // ========== 減持測試 ==========

    @Nested
    @DisplayName("減持識別")
    class DecreasedTest {

        @Test
        @DisplayName("識別減持股票 - 股數減少")
        void compare_WhenSharesDecreased_ShouldIdentifyAsDecreased() {
            List<Holding> startHoldings = List.of(
                    createHolding("2330", "台積電", 1000000L, "25.00"));
            List<Holding> endHoldings = List.of(
                    createHolding("2330", "台積電", 800000L, "22.00"));

            DailySnapshot startSnapshot = createSnapshot(startDate, startHoldings);
            DailySnapshot endSnapshot = createSnapshot(endDate, endHoldings);

            when(excelStorageService.getSnapshot(startDate)).thenReturn(Optional.of(startSnapshot));
            when(excelStorageService.getSnapshot(endDate)).thenReturn(Optional.of(endSnapshot));

            RangeCompareResultDto result = holdingCompareService.compareHoldings(startDate, endDate);

            assertThat(result.decreased()).hasSize(1);
            assertThat(result.decreased().get(0).stockCode()).isEqualTo("2330");
            assertThat(result.decreased().get(0).changeType()).isEqualTo(ChangeType.DECREASED);
            assertThat(result.decreased().get(0).startShares()).isEqualTo(1000000L);
            assertThat(result.decreased().get(0).endShares()).isEqualTo(800000L);
            assertThat(result.decreased().get(0).sharesDiff()).isEqualTo(-200000L);
        }

        @Test
        @DisplayName("減持變化比例計算正確 - 應為負值")
        void compare_WhenDecreased_ChangeRatioShouldBeNegative() {
            List<Holding> startHoldings = List.of(
                    createHolding("2330", "台積電", 1000000L, "25.00"));
            List<Holding> endHoldings = List.of(
                    createHolding("2330", "台積電", 800000L, "22.00"));

            DailySnapshot startSnapshot = createSnapshot(startDate, startHoldings);
            DailySnapshot endSnapshot = createSnapshot(endDate, endHoldings);

            when(excelStorageService.getSnapshot(startDate)).thenReturn(Optional.of(startSnapshot));
            when(excelStorageService.getSnapshot(endDate)).thenReturn(Optional.of(endSnapshot));

            RangeCompareResultDto result = holdingCompareService.compareHoldings(startDate, endDate);

            // 變化比例 = (800000 - 1000000) / 1000000 * 100 = -20%
            assertThat(result.decreased().get(0).changeRatio())
                    .isEqualByComparingTo(new BigDecimal("-20.00"));
        }

        @Test
        @DisplayName("減持權重變化計算正確 - 應為負值")
        void compare_WhenDecreased_WeightDiffShouldBeNegative() {
            List<Holding> startHoldings = List.of(
                    createHolding("2330", "台積電", 1000000L, "25.00"));
            List<Holding> endHoldings = List.of(
                    createHolding("2330", "台積電", 800000L, "20.00"));

            DailySnapshot startSnapshot = createSnapshot(startDate, startHoldings);
            DailySnapshot endSnapshot = createSnapshot(endDate, endHoldings);

            when(excelStorageService.getSnapshot(startDate)).thenReturn(Optional.of(startSnapshot));
            when(excelStorageService.getSnapshot(endDate)).thenReturn(Optional.of(endSnapshot));

            RangeCompareResultDto result = holdingCompareService.compareHoldings(startDate, endDate);

            assertThat(result.decreased().get(0).weightDiff())
                    .isEqualByComparingTo(new BigDecimal("-5.00"));
        }
    }

    // ========== 不變測試 ==========

    @Nested
    @DisplayName("不變識別")
    class UnchangedTest {

        @Test
        @DisplayName("識別不變股票 - 股數相同")
        void compare_WhenSharesUnchanged_ShouldIdentifyAsUnchanged() {
            List<Holding> startHoldings = List.of(
                    createHolding("2330", "台積電", 1000000L, "25.00"));
            List<Holding> endHoldings = List.of(
                    createHolding("2330", "台積電", 1000000L, "25.50"));

            DailySnapshot startSnapshot = createSnapshot(startDate, startHoldings);
            DailySnapshot endSnapshot = createSnapshot(endDate, endHoldings);

            when(excelStorageService.getSnapshot(startDate)).thenReturn(Optional.of(startSnapshot));
            when(excelStorageService.getSnapshot(endDate)).thenReturn(Optional.of(endSnapshot));

            RangeCompareResultDto result = holdingCompareService.compareHoldings(startDate, endDate);

            assertThat(result.unchanged()).hasSize(1);
            assertThat(result.unchanged().get(0).stockCode()).isEqualTo("2330");
            assertThat(result.unchanged().get(0).changeType()).isEqualTo(ChangeType.UNCHANGED);
            assertThat(result.unchanged().get(0).sharesDiff()).isEqualTo(0L);
            assertThat(result.unchanged().get(0).changeRatio())
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("不變股票仍可有權重變化")
        void compare_WhenUnchanged_CanHaveWeightChange() {
            List<Holding> startHoldings = List.of(
                    createHolding("2330", "台積電", 1000000L, "25.00"));
            List<Holding> endHoldings = List.of(
                    createHolding("2330", "台積電", 1000000L, "27.00"));

            DailySnapshot startSnapshot = createSnapshot(startDate, startHoldings);
            DailySnapshot endSnapshot = createSnapshot(endDate, endHoldings);

            when(excelStorageService.getSnapshot(startDate)).thenReturn(Optional.of(startSnapshot));
            when(excelStorageService.getSnapshot(endDate)).thenReturn(Optional.of(endSnapshot));

            RangeCompareResultDto result = holdingCompareService.compareHoldings(startDate, endDate);

            assertThat(result.unchanged()).hasSize(1);
            assertThat(result.unchanged().get(0).weightDiff())
                    .isEqualByComparingTo(new BigDecimal("2.00"));
        }
    }

    // ========== 綜合測試 ==========

    @Nested
    @DisplayName("綜合場景測試")
    class ComprehensiveTest {

        @Test
        @DisplayName("混合變化場景 - 新進、剔除、增持、減持、不變同時存在")
        void compare_MixedChanges_ShouldClassifyCorrectly() {
            List<Holding> startHoldings = List.of(
                    createHolding("2330", "台積電", 1000000L, "25.00"), // 增持
                    createHolding("2317", "鴻海", 500000L, "12.00"), // 減持
                    createHolding("2454", "聯發科", 300000L, "8.00"), // 不變
                    createHolding("2412", "中華電", 200000L, "5.00")); // 剔除

            List<Holding> endHoldings = List.of(
                    createHolding("2330", "台積電", 1200000L, "27.00"), // 增持
                    createHolding("2317", "鴻海", 400000L, "10.00"), // 減持
                    createHolding("2454", "聯發科", 300000L, "8.50"), // 不變
                    createHolding("2308", "台達電", 250000L, "6.00")); // 新進

            DailySnapshot startSnapshot = createSnapshot(startDate, startHoldings);
            DailySnapshot endSnapshot = createSnapshot(endDate, endHoldings);

            when(excelStorageService.getSnapshot(startDate)).thenReturn(Optional.of(startSnapshot));
            when(excelStorageService.getSnapshot(endDate)).thenReturn(Optional.of(endSnapshot));

            RangeCompareResultDto result = holdingCompareService.compareHoldings(startDate, endDate);

            // 驗證分類
            assertThat(result.newAdditions()).hasSize(1);
            assertThat(result.removals()).hasSize(1);
            assertThat(result.increased()).hasSize(1);
            assertThat(result.decreased()).hasSize(1);
            assertThat(result.unchanged()).hasSize(1);

            // 驗證新進
            assertThat(result.newAdditions().get(0).stockCode()).isEqualTo("2308");

            // 驗證剔除
            assertThat(result.removals().get(0).stockCode()).isEqualTo("2412");

            // 驗證增持
            assertThat(result.increased().get(0).stockCode()).isEqualTo("2330");

            // 驗證減持
            assertThat(result.decreased().get(0).stockCode()).isEqualTo("2317");

            // 驗證不變
            assertThat(result.unchanged().get(0).stockCode()).isEqualTo("2454");
        }

        @Test
        @DisplayName("空資料集合 - 起始與結束都無資料")
        void compare_EmptySnapshots_ShouldReturnEmptyResults() {
            List<Holding> emptyHoldings = List.of();

            DailySnapshot startSnapshot = createSnapshot(startDate, emptyHoldings);
            DailySnapshot endSnapshot = createSnapshot(endDate, emptyHoldings);

            when(excelStorageService.getSnapshot(startDate)).thenReturn(Optional.of(startSnapshot));
            when(excelStorageService.getSnapshot(endDate)).thenReturn(Optional.of(endSnapshot));

            RangeCompareResultDto result = holdingCompareService.compareHoldings(startDate, endDate);

            assertThat(result.newAdditions()).isEmpty();
            assertThat(result.removals()).isEmpty();
            assertThat(result.increased()).isEmpty();
            assertThat(result.decreased()).isEmpty();
            assertThat(result.unchanged()).isEmpty();
        }

        @Test
        @DisplayName("全部新進 - 起始無資料，結束有資料")
        void compare_AllNewAdditions_ShouldClassifyAllAsNew() {
            List<Holding> startHoldings = List.of();
            List<Holding> endHoldings = List.of(
                    createHolding("2330", "台積電", 1000000L, "25.00"),
                    createHolding("2454", "聯發科", 500000L, "15.00"));

            DailySnapshot startSnapshot = createSnapshot(startDate, startHoldings);
            DailySnapshot endSnapshot = createSnapshot(endDate, endHoldings);

            when(excelStorageService.getSnapshot(startDate)).thenReturn(Optional.of(startSnapshot));
            when(excelStorageService.getSnapshot(endDate)).thenReturn(Optional.of(endSnapshot));

            RangeCompareResultDto result = holdingCompareService.compareHoldings(startDate, endDate);

            assertThat(result.newAdditions()).hasSize(2);
            assertThat(result.removals()).isEmpty();
            assertThat(result.increased()).isEmpty();
            assertThat(result.decreased()).isEmpty();
            assertThat(result.unchanged()).isEmpty();
        }

        @Test
        @DisplayName("全部剔除 - 起始有資料，結束無資料")
        void compare_AllRemovals_ShouldClassifyAllAsRemoved() {
            List<Holding> startHoldings = List.of(
                    createHolding("2330", "台積電", 1000000L, "25.00"),
                    createHolding("2454", "聯發科", 500000L, "15.00"));
            List<Holding> endHoldings = List.of();

            DailySnapshot startSnapshot = createSnapshot(startDate, startHoldings);
            DailySnapshot endSnapshot = createSnapshot(endDate, endHoldings);

            when(excelStorageService.getSnapshot(startDate)).thenReturn(Optional.of(startSnapshot));
            when(excelStorageService.getSnapshot(endDate)).thenReturn(Optional.of(endSnapshot));

            RangeCompareResultDto result = holdingCompareService.compareHoldings(startDate, endDate);

            assertThat(result.newAdditions()).isEmpty();
            assertThat(result.removals()).hasSize(2);
            assertThat(result.increased()).isEmpty();
            assertThat(result.decreased()).isEmpty();
            assertThat(result.unchanged()).isEmpty();
        }
    }

    // ========== 回傳值結構測試 ==========

    @Nested
    @DisplayName("回傳值結構")
    class ReturnStructureTest {

        @Test
        @DisplayName("回傳結果應包含正確的起始與結束日期")
        void compare_ShouldReturnCorrectDates() {
            List<Holding> holdings = List.of(
                    createHolding("2330", "台積電", 1000000L, "25.00"));

            DailySnapshot startSnapshot = createSnapshot(startDate, holdings);
            DailySnapshot endSnapshot = createSnapshot(endDate, holdings);

            when(excelStorageService.getSnapshot(startDate)).thenReturn(Optional.of(startSnapshot));
            when(excelStorageService.getSnapshot(endDate)).thenReturn(Optional.of(endSnapshot));

            RangeCompareResultDto result = holdingCompareService.compareHoldings(startDate, endDate);

            assertThat(result.startDate()).isEqualTo(startDate);
            assertThat(result.endDate()).isEqualTo(endDate);
        }

        @Test
        @DisplayName("計數方法應正確計算各分類數量")
        void compare_CountMethods_ShouldReturnCorrectCounts() {
            List<Holding> startHoldings = List.of(
                    createHolding("2330", "台積電", 1000000L, "25.00"),
                    createHolding("2412", "中華電", 200000L, "5.00"));

            List<Holding> endHoldings = List.of(
                    createHolding("2330", "台積電", 1200000L, "27.00"),
                    createHolding("2454", "聯發科", 500000L, "15.00"));

            DailySnapshot startSnapshot = createSnapshot(startDate, startHoldings);
            DailySnapshot endSnapshot = createSnapshot(endDate, endHoldings);

            when(excelStorageService.getSnapshot(startDate)).thenReturn(Optional.of(startSnapshot));
            when(excelStorageService.getSnapshot(endDate)).thenReturn(Optional.of(endSnapshot));

            RangeCompareResultDto result = holdingCompareService.compareHoldings(startDate, endDate);

            assertThat(result.newAdditionsCount()).isEqualTo(1);
            assertThat(result.removalsCount()).isEqualTo(1);
            assertThat(result.increasedCount()).isEqualTo(1);
            assertThat(result.decreasedCount()).isEqualTo(0);
            assertThat(result.unchangedCount()).isEqualTo(0);
        }
    }

    // ========== 精度測試 ==========

    @Nested
    @DisplayName("數值精度")
    class PrecisionTest {

        @Test
        @DisplayName("變化比例應保留 2 位小數")
        void compare_ChangeRatio_ShouldHave2DecimalPlaces() {
            List<Holding> startHoldings = List.of(
                    createHolding("2330", "台積電", 1000000L, "25.0000"));
            List<Holding> endHoldings = List.of(
                    createHolding("2330", "台積電", 1333333L, "30.0000"));

            DailySnapshot startSnapshot = createSnapshot(startDate, startHoldings);
            DailySnapshot endSnapshot = createSnapshot(endDate, endHoldings);

            when(excelStorageService.getSnapshot(startDate)).thenReturn(Optional.of(startSnapshot));
            when(excelStorageService.getSnapshot(endDate)).thenReturn(Optional.of(endSnapshot));

            RangeCompareResultDto result = holdingCompareService.compareHoldings(startDate, endDate);

            // 變化比例 = 33.3333... 應四捨五入為 33.33
            assertThat(result.increased().get(0).changeRatio().scale()).isLessThanOrEqualTo(2);
        }

        @Test
        @DisplayName("權重變化應保留 4 位小數")
        void compare_WeightDiff_ShouldHave4DecimalPlaces() {
            List<Holding> startHoldings = List.of(
                    createHolding("2330", "台積電", 1000000L, "25.1234"));
            List<Holding> endHoldings = List.of(
                    createHolding("2330", "台積電", 1200000L, "28.5678"));

            DailySnapshot startSnapshot = createSnapshot(startDate, startHoldings);
            DailySnapshot endSnapshot = createSnapshot(endDate, endHoldings);

            when(excelStorageService.getSnapshot(startDate)).thenReturn(Optional.of(startSnapshot));
            when(excelStorageService.getSnapshot(endDate)).thenReturn(Optional.of(endSnapshot));

            RangeCompareResultDto result = holdingCompareService.compareHoldings(startDate, endDate);

            // 權重變化精度應足以表示原始精度
            BigDecimal expectedWeightDiff = new BigDecimal("28.5678")
                    .subtract(new BigDecimal("25.1234"));
            assertThat(result.increased().get(0).weightDiff())
                    .isEqualByComparingTo(expectedWeightDiff);
        }
    }
}
