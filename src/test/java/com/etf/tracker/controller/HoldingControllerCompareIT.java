package com.etf.tracker.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.etf.tracker.config.AppConfig;
import com.etf.tracker.model.DailySnapshot;
import com.etf.tracker.model.Holding;
import com.etf.tracker.service.StorageService;

/**
 * HoldingController 比較功能整合測試
 * <p>
 * 測試 User Story 3 的比較端點 GET /api/holdings/compare
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HoldingControllerCompareIT {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private StorageService storageService;

        @Autowired
        private AppConfig appConfig;

        private LocalDate startDate;
        private LocalDate endDate;

        @BeforeEach
        void setUp() {
                // 清除現有資料
                try {
                        Path storagePath = Path.of(appConfig.getData().getStoragePath());
                        Path filePath = storagePath.resolve(appConfig.getData().getFileName());
                        Files.deleteIfExists(filePath);
                } catch (IOException ignored) {
                }

                // 設定測試日期
                startDate = LocalDate.of(2024, 1, 10);
                endDate = LocalDate.of(2024, 1, 15);
        }

        /**
         * 建立測試資料
         */
        private void setupTestData() {
                // 起始日資料
                DailySnapshot startSnapshot = DailySnapshot.builder()
                                .date(startDate)
                                .holdings(List.of(
                                                Holding.builder()
                                                                .stockCode("2330")
                                                                .stockName("台積電")
                                                                .shares(1000000L)
                                                                .weight(new BigDecimal("25.00"))
                                                                .build(),
                                                Holding.builder()
                                                                .stockCode("2317")
                                                                .stockName("鴻海")
                                                                .shares(500000L)
                                                                .weight(new BigDecimal("12.00"))
                                                                .build(),
                                                Holding.builder()
                                                                .stockCode("2412")
                                                                .stockName("中華電")
                                                                .shares(300000L)
                                                                .weight(new BigDecimal("8.00"))
                                                                .build()))
                                .totalCount(3)
                                .totalWeight(new BigDecimal("45.00"))
                                .build();

                // 結束日資料
                DailySnapshot endSnapshot = DailySnapshot.builder()
                                .date(endDate)
                                .holdings(List.of(
                                                Holding.builder()
                                                                .stockCode("2330")
                                                                .stockName("台積電")
                                                                .shares(1200000L) // 增持
                                                                .weight(new BigDecimal("28.00"))
                                                                .build(),
                                                Holding.builder()
                                                                .stockCode("2317")
                                                                .stockName("鴻海")
                                                                .shares(400000L) // 減持
                                                                .weight(new BigDecimal("10.00"))
                                                                .build(),
                                                Holding.builder()
                                                                .stockCode("2454")
                                                                .stockName("聯發科")
                                                                .shares(250000L) // 新進
                                                                .weight(new BigDecimal("7.00"))
                                                                .build()))
                                .totalCount(3)
                                .totalWeight(new BigDecimal("45.00"))
                                .build();
                // 中華電被剔除

                storageService.saveSnapshot(startSnapshot);
                storageService.saveSnapshot(endSnapshot);
        }

        @Nested
        @DisplayName("成功場景")
        class SuccessScenarios {

                @Test
                @Order(1)
                @DisplayName("GET /api/holdings/compare - 比較兩日期間的持倉變化")
                void compareHoldings_ShouldReturnCompareResult() throws Exception {
                        setupTestData();

                        mockMvc.perform(get("/api/holdings/compare")
                                        .param("startDate", startDate.toString())
                                        .param("endDate", endDate.toString()))
                                        .andExpect(status().isOk())
                                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                        .andExpect(jsonPath("$.success").value(true))
                                        .andExpect(jsonPath("$.data.startDate").value(startDate.toString()))
                                        .andExpect(jsonPath("$.data.endDate").value(endDate.toString()));
                }

                @Test
                @Order(2)
                @DisplayName("比較結果包含正確的新進增持資料")
                void compareHoldings_ShouldContainNewAdditions() throws Exception {
                        setupTestData();

                        mockMvc.perform(get("/api/holdings/compare")
                                        .param("startDate", startDate.toString())
                                        .param("endDate", endDate.toString()))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.data.newAdditions").isArray())
                                        .andExpect(jsonPath("$.data.newAdditions.length()").value(1))
                                        .andExpect(jsonPath("$.data.newAdditions[0].stockCode").value("2454"))
                                        .andExpect(jsonPath("$.data.newAdditions[0].stockName").value("聯發科"))
                                        .andExpect(jsonPath("$.data.newAdditions[0].changeType").value("NEW_ADDITION"))
                                        .andExpect(jsonPath("$.data.newAdditions[0].endShares").value(250000));
                }

                @Test
                @Order(3)
                @DisplayName("比較結果包含正確的剔除減持資料")
                void compareHoldings_ShouldContainRemovals() throws Exception {
                        setupTestData();

                        mockMvc.perform(get("/api/holdings/compare")
                                        .param("startDate", startDate.toString())
                                        .param("endDate", endDate.toString()))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.data.removals").isArray())
                                        .andExpect(jsonPath("$.data.removals.length()").value(1))
                                        .andExpect(jsonPath("$.data.removals[0].stockCode").value("2412"))
                                        .andExpect(jsonPath("$.data.removals[0].stockName").value("中華電"))
                                        .andExpect(jsonPath("$.data.removals[0].changeType").value("REMOVED"))
                                        .andExpect(jsonPath("$.data.removals[0].startShares").value(300000));
                }

                @Test
                @Order(4)
                @DisplayName("比較結果包含正確的增持資料")
                void compareHoldings_ShouldContainIncreased() throws Exception {
                        setupTestData();

                        mockMvc.perform(get("/api/holdings/compare")
                                        .param("startDate", startDate.toString())
                                        .param("endDate", endDate.toString()))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.data.increased").isArray())
                                        .andExpect(jsonPath("$.data.increased.length()").value(1))
                                        .andExpect(jsonPath("$.data.increased[0].stockCode").value("2330"))
                                        .andExpect(jsonPath("$.data.increased[0].changeType").value("INCREASED"))
                                        .andExpect(jsonPath("$.data.increased[0].startShares").value(1000000))
                                        .andExpect(jsonPath("$.data.increased[0].endShares").value(1200000))
                                        .andExpect(jsonPath("$.data.increased[0].sharesDiff").value(200000));
                }

                @Test
                @Order(5)
                @DisplayName("比較結果包含正確的減持資料")
                void compareHoldings_ShouldContainDecreased() throws Exception {
                        setupTestData();

                        mockMvc.perform(get("/api/holdings/compare")
                                        .param("startDate", startDate.toString())
                                        .param("endDate", endDate.toString()))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.data.decreased").isArray())
                                        .andExpect(jsonPath("$.data.decreased.length()").value(1))
                                        .andExpect(jsonPath("$.data.decreased[0].stockCode").value("2317"))
                                        .andExpect(jsonPath("$.data.decreased[0].changeType").value("DECREASED"))
                                        .andExpect(jsonPath("$.data.decreased[0].startShares").value(500000))
                                        .andExpect(jsonPath("$.data.decreased[0].endShares").value(400000))
                                        .andExpect(jsonPath("$.data.decreased[0].sharesDiff").value(-100000));
                }

                @Test
                @Order(6)
                @DisplayName("同日期比較應回傳所有股票為不變")
                void compareHoldings_SameDate_ShouldReturnAllUnchanged() throws Exception {
                        setupTestData();

                        mockMvc.perform(get("/api/holdings/compare")
                                        .param("startDate", startDate.toString())
                                        .param("endDate", startDate.toString()))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.data.newAdditions").isEmpty())
                                        .andExpect(jsonPath("$.data.removals").isEmpty())
                                        .andExpect(jsonPath("$.data.increased").isEmpty())
                                        .andExpect(jsonPath("$.data.decreased").isEmpty())
                                        .andExpect(jsonPath("$.data.unchanged.length()").value(3));
                }

                @Test
                @Order(7)
                @DisplayName("增持變化比例計算正確")
                void compareHoldings_ChangeRatioCalculation() throws Exception {
                        setupTestData();

                        mockMvc.perform(get("/api/holdings/compare")
                                        .param("startDate", startDate.toString())
                                        .param("endDate", endDate.toString()))
                                        .andExpect(status().isOk())
                                        // 台積電: (1200000 - 1000000) / 1000000 * 100 = 20%
                                        .andExpect(jsonPath("$.data.increased[0].changeRatio").value(20.00));
                }

                @Test
                @Order(8)
                @DisplayName("減持變化比例為負值")
                void compareHoldings_NegativeChangeRatio() throws Exception {
                        setupTestData();

                        mockMvc.perform(get("/api/holdings/compare")
                                        .param("startDate", startDate.toString())
                                        .param("endDate", endDate.toString()))
                                        .andExpect(status().isOk())
                                        // 鴻海: (400000 - 500000) / 500000 * 100 = -20%
                                        .andExpect(jsonPath("$.data.decreased[0].changeRatio").value(-20.00));
                }
        }

        @Nested
        @DisplayName("錯誤場景")
        class ErrorScenarios {

                @Test
                @Order(10)
                @DisplayName("起始日期晚於結束日期應回傳錯誤")
                void compareHoldings_InvalidDateRange_ShouldReturnBadRequest() throws Exception {
                        setupTestData();

                        mockMvc.perform(get("/api/holdings/compare")
                                        .param("startDate", endDate.toString())
                                        .param("endDate", startDate.toString()))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.success").value(false));
                }

                @Test
                @Order(11)
                @DisplayName("起始日期無資料應回傳錯誤")
                void compareHoldings_StartDateNotFound_ShouldReturnBadRequest() throws Exception {
                        // 只建立結束日資料
                        DailySnapshot endSnapshot = DailySnapshot.builder()
                                        .date(endDate)
                                        .holdings(List.of(
                                                        Holding.builder()
                                                                        .stockCode("2330")
                                                                        .stockName("台積電")
                                                                        .shares(1000000L)
                                                                        .weight(new BigDecimal("25.00"))
                                                                        .build()))
                                        .totalCount(1)
                                        .totalWeight(new BigDecimal("25.00"))
                                        .build();
                        storageService.saveSnapshot(endSnapshot);

                        mockMvc.perform(get("/api/holdings/compare")
                                        .param("startDate", startDate.toString())
                                        .param("endDate", endDate.toString()))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.success").value(false));
                }

                @Test
                @Order(12)
                @DisplayName("結束日期無資料應回傳錯誤")
                void compareHoldings_EndDateNotFound_ShouldReturnBadRequest() throws Exception {
                        // 只建立起始日資料
                        DailySnapshot startSnapshot = DailySnapshot.builder()
                                        .date(startDate)
                                        .holdings(List.of(
                                                        Holding.builder()
                                                                        .stockCode("2330")
                                                                        .stockName("台積電")
                                                                        .shares(1000000L)
                                                                        .weight(new BigDecimal("25.00"))
                                                                        .build()))
                                        .totalCount(1)
                                        .totalWeight(new BigDecimal("25.00"))
                                        .build();
                        storageService.saveSnapshot(startSnapshot);

                        mockMvc.perform(get("/api/holdings/compare")
                                        .param("startDate", startDate.toString())
                                        .param("endDate", endDate.toString()))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.success").value(false));
                }

                @Test
                @Order(13)
                @DisplayName("缺少必要參數應回傳錯誤")
                void compareHoldings_MissingParams_ShouldReturnBadRequest() throws Exception {
                        mockMvc.perform(get("/api/holdings/compare")
                                        .param("startDate", startDate.toString()))
                                        // 缺少 endDate
                                        .andExpect(status().isBadRequest());
                }
        }

        @Nested
        @DisplayName("邊界場景")
        class BoundaryScenarios {

                @Test
                @Order(20)
                @DisplayName("全部新進 - 起始日無資料")
                void compareHoldings_AllNewAdditions() throws Exception {
                        // 起始日無股票
                        DailySnapshot startSnapshot = DailySnapshot.builder()
                                        .date(startDate)
                                        .holdings(List.of())
                                        .totalCount(0)
                                        .totalWeight(BigDecimal.ZERO)
                                        .build();

                        DailySnapshot endSnapshot = DailySnapshot.builder()
                                        .date(endDate)
                                        .holdings(List.of(
                                                        Holding.builder()
                                                                        .stockCode("2330")
                                                                        .stockName("台積電")
                                                                        .shares(1000000L)
                                                                        .weight(new BigDecimal("25.00"))
                                                                        .build()))
                                        .totalCount(1)
                                        .totalWeight(new BigDecimal("25.00"))
                                        .build();

                        storageService.saveSnapshot(startSnapshot);
                        storageService.saveSnapshot(endSnapshot);

                        mockMvc.perform(get("/api/holdings/compare")
                                        .param("startDate", startDate.toString())
                                        .param("endDate", endDate.toString()))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.data.newAdditions.length()").value(1))
                                        .andExpect(jsonPath("$.data.removals").isEmpty())
                                        .andExpect(jsonPath("$.data.increased").isEmpty())
                                        .andExpect(jsonPath("$.data.decreased").isEmpty())
                                        .andExpect(jsonPath("$.data.unchanged").isEmpty());
                }

                @Test
                @Order(21)
                @DisplayName("全部剔除 - 結束日無資料")
                void compareHoldings_AllRemovals() throws Exception {
                        DailySnapshot startSnapshot = DailySnapshot.builder()
                                        .date(startDate)
                                        .holdings(List.of(
                                                        Holding.builder()
                                                                        .stockCode("2330")
                                                                        .stockName("台積電")
                                                                        .shares(1000000L)
                                                                        .weight(new BigDecimal("25.00"))
                                                                        .build()))
                                        .totalCount(1)
                                        .totalWeight(new BigDecimal("25.00"))
                                        .build();

                        // 結束日無股票
                        DailySnapshot endSnapshot = DailySnapshot.builder()
                                        .date(endDate)
                                        .holdings(List.of())
                                        .totalCount(0)
                                        .totalWeight(BigDecimal.ZERO)
                                        .build();

                        storageService.saveSnapshot(startSnapshot);
                        storageService.saveSnapshot(endSnapshot);

                        mockMvc.perform(get("/api/holdings/compare")
                                        .param("startDate", startDate.toString())
                                        .param("endDate", endDate.toString()))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.data.newAdditions").isEmpty())
                                        .andExpect(jsonPath("$.data.removals.length()").value(1))
                                        .andExpect(jsonPath("$.data.removals[0].changeRatio").value(-100.00))
                                        .andExpect(jsonPath("$.data.increased").isEmpty())
                                        .andExpect(jsonPath("$.data.decreased").isEmpty())
                                        .andExpect(jsonPath("$.data.unchanged").isEmpty());
                }
        }
}
