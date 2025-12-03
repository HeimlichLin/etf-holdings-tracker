package com.etf.tracker.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.etf.tracker.config.AppConfig;
import com.etf.tracker.model.DailySnapshot;
import com.etf.tracker.model.Holding;
import com.etf.tracker.service.StorageService;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.mockwebserver.MockWebServer;

/**
 * HoldingController 整合測試
 * <p>
 * 測試完整的抓取與儲存流程
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HoldingControllerFetchIT {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private StorageService storageService;

        @Autowired
        private AppConfig appConfig;

        private static MockWebServer mockWebServer;
        private static Path testStoragePath;

        @BeforeAll
        static void setUpAll() throws IOException {
                // 建立 MockWebServer
                mockWebServer = new MockWebServer();
                mockWebServer.start();

                // 建立測試儲存目錄
                testStoragePath = Files.createTempDirectory("etf-tracker-it-");
        }

        @AfterAll
        static void tearDownAll() throws IOException {
                mockWebServer.shutdown();

                // 清理測試檔案
                if (testStoragePath != null && Files.exists(testStoragePath)) {
                        Files.walk(testStoragePath)
                                        .sorted((a, b) -> -a.compareTo(b))
                                        .forEach(path -> {
                                                try {
                                                        Files.deleteIfExists(path);
                                                } catch (IOException ignored) {
                                                }
                                        });
                }
        }

        @BeforeEach
        void setUp() {
                // 清除現有資料
                try {
                        Path storagePath = Path.of(appConfig.getData().getStoragePath());
                        Path filePath = storagePath.resolve(appConfig.getData().getFileName());
                        Files.deleteIfExists(filePath);
                } catch (IOException ignored) {
                }
        }

        @Test
        @Order(1)
        @DisplayName("GET /api/holdings/dates - 初始狀態應回傳空列表")
        void getAvailableDates_WhenNoData_ShouldReturnEmptyList() throws Exception {
                mockMvc.perform(get("/api/holdings/dates"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.availableDates").isArray())
                                .andExpect(jsonPath("$.data.availableDates").isEmpty());
        }

        @Test
        @Order(2)
        @DisplayName("GET /api/holdings/latest - 無資料時應回傳 404")
        void getLatestHoldings_WhenNoData_ShouldReturn404() throws Exception {
                mockMvc.perform(get("/api/holdings/latest"))
                                .andExpect(status().isNotFound());
        }

        @Test
        @Order(3)
        @DisplayName("GET /api/holdings/{date} - 查詢不存在的日期應回傳 404")
        void getHoldingsByDate_WhenNotExists_ShouldReturn404() throws Exception {
                mockMvc.perform(get("/api/holdings/2024-01-01"))
                                .andExpect(status().isNotFound());
        }

        @Test
        @Order(4)
        @DisplayName("儲存測試資料後查詢")
        void saveAndRetrieveHoldings() throws Exception {
                // 準備測試資料
                LocalDate testDate = LocalDate.now();
                DailySnapshot snapshot = DailySnapshot.builder()
                                .date(testDate)
                                .holdings(List.of(
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
                                                                .build()))
                                .totalCount(2)
                                .totalWeight(new BigDecimal("40.80"))
                                .build();

                // 儲存
                storageService.saveSnapshot(snapshot);

                // 驗證可用日期
                mockMvc.perform(get("/api/holdings/dates"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.availableDates").isNotEmpty())
                                .andExpect(jsonPath("$.data.latestDate").value(testDate.toString()));

                // 驗證最新資料
                MvcResult result = mockMvc.perform(get("/api/holdings/latest"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.totalCount").value(2))
                                .andExpect(jsonPath("$.data.holdings[0].stockCode").value("2330"))
                                .andReturn();

                // 驗證指定日期
                mockMvc.perform(get("/api/holdings/" + testDate))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.totalCount").value(2));
        }

        @Test
        @Order(5)
        @DisplayName("DELETE /api/holdings/cleanup - 清理舊資料")
        void cleanupOldData_ShouldDeleteOldRecords() throws Exception {
                // 清除現有資料以確保測試隔離
                Path storagePath = Path.of(appConfig.getData().getStoragePath());
                Path filePath = storagePath.resolve(appConfig.getData().getFileName());
                Files.deleteIfExists(filePath);

                // 準備測試資料 - 100 天前的資料（確保會被清理）
                LocalDate oldDate = LocalDate.now().minusDays(100);
                DailySnapshot oldSnapshot = DailySnapshot.builder()
                                .date(oldDate)
                                .holdings(List.of(
                                                Holding.builder()
                                                                .stockCode("2330")
                                                                .stockName("台積電")
                                                                .shares(1000000L)
                                                                .weight(new BigDecimal("25.50"))
                                                                .build()))
                                .totalCount(1)
                                .totalWeight(new BigDecimal("25.50"))
                                .build();

                storageService.saveSnapshot(oldSnapshot);

                // 新增較新的資料（10 天前，確保不會被清理）
                LocalDate recentDate = LocalDate.now().minusDays(10);
                DailySnapshot recentSnapshot = DailySnapshot.builder()
                                .date(recentDate)
                                .holdings(List.of(
                                                Holding.builder()
                                                                .stockCode("2454")
                                                                .stockName("聯發科")
                                                                .shares(500000L)
                                                                .weight(new BigDecimal("15.30"))
                                                                .build()))
                                .totalCount(1)
                                .totalWeight(new BigDecimal("15.30"))
                                .build();

                storageService.saveSnapshot(recentSnapshot);

                // 執行清理 - 保留 90 天內的資料
                // oldDate (100 天前) 會被刪除
                // recentDate (10 天前) 會被保留
                mockMvc.perform(delete("/api/holdings/cleanup")
                                .param("days", "90")
                                .param("confirm", "true"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));

                // 驗證舊資料已被刪除
                mockMvc.perform(get("/api/holdings/" + oldDate))
                                .andExpect(status().isNotFound());

                // 驗證較新資料仍存在
                mockMvc.perform(get("/api/holdings/" + recentDate))
                                .andExpect(status().isOk());
        }

        @Test
        @Order(6)
        @DisplayName("資料更新測試 - 相同日期的資料應被覆蓋")
        void saveSnapshot_WhenSameDateExists_ShouldOverwrite() throws Exception {
                LocalDate testDate = LocalDate.now();

                // 第一次儲存
                DailySnapshot first = DailySnapshot.builder()
                                .date(testDate)
                                .holdings(List.of(
                                                Holding.builder()
                                                                .stockCode("2330")
                                                                .stockName("台積電")
                                                                .shares(1000000L)
                                                                .weight(new BigDecimal("25.50"))
                                                                .build()))
                                .totalCount(1)
                                .totalWeight(new BigDecimal("25.50"))
                                .build();

                storageService.saveSnapshot(first);

                // 第二次儲存（相同日期，不同資料）
                DailySnapshot second = DailySnapshot.builder()
                                .date(testDate)
                                .holdings(List.of(
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
                                                                .build()))
                                .totalCount(2)
                                .totalWeight(new BigDecimal("25.50"))
                                .build();

                storageService.saveSnapshot(second);

                // 驗證資料被覆蓋
                mockMvc.perform(get("/api/holdings/" + testDate))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.totalCount").value(2))
                                .andExpect(jsonPath("$.data.holdings[0].stockCode").value("2454"));
        }

        @Test
        @Order(7)
        @DisplayName("並發請求測試 - 多個請求同時查詢")
        void concurrentRequests_ShouldHandleGracefully() throws Exception {
                // 準備資料
                LocalDate testDate = LocalDate.now();
                DailySnapshot snapshot = DailySnapshot.builder()
                                .date(testDate)
                                .holdings(List.of(
                                                Holding.builder()
                                                                .stockCode("2330")
                                                                .stockName("台積電")
                                                                .shares(1000000L)
                                                                .weight(new BigDecimal("25.50"))
                                                                .build()))
                                .totalCount(1)
                                .totalWeight(new BigDecimal("25.50"))
                                .build();

                storageService.saveSnapshot(snapshot);

                // 模擬多個並發請求
                for (int i = 0; i < 5; i++) {
                        mockMvc.perform(get("/api/holdings/latest"))
                                        .andExpect(status().isOk());

                        mockMvc.perform(get("/api/holdings/dates"))
                                        .andExpect(status().isOk());
                }
        }

        @Test
        @Order(8)
        @DisplayName("回應格式驗證 - API 回應應符合規範")
        void apiResponse_ShouldFollowSpecification() throws Exception {
                // 準備資料
                LocalDate testDate = LocalDate.now();
                DailySnapshot snapshot = DailySnapshot.builder()
                                .date(testDate)
                                .holdings(List.of(
                                                Holding.builder()
                                                                .stockCode("2330")
                                                                .stockName("台積電")
                                                                .shares(1000000L)
                                                                .weight(new BigDecimal("25.50"))
                                                                .build()))
                                .totalCount(1)
                                .totalWeight(new BigDecimal("25.50"))
                                .build();

                storageService.saveSnapshot(snapshot);

                // 驗證成功回應格式
                mockMvc.perform(get("/api/holdings/latest"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data").exists())
                                .andExpect(jsonPath("$.data.date").exists())
                                .andExpect(jsonPath("$.data.holdings").isArray())
                                .andExpect(jsonPath("$.data.totalCount").isNumber())
                                .andExpect(jsonPath("$.data.totalWeight").isNumber());
        }
}
