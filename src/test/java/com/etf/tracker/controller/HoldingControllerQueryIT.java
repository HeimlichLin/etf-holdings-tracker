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

import com.etf.tracker.config.AppConfig;
import com.etf.tracker.model.DailySnapshot;
import com.etf.tracker.model.Holding;
import com.etf.tracker.service.ExcelStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * HoldingController 查詢功能整合測試
 * <p>
 * 測試 User Story 2 的查詢端點
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HoldingControllerQueryIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExcelStorageService excelStorageService;

    @Autowired
    private AppConfig appConfig;

    private static LocalDate testDate;
    private static DailySnapshot testSnapshot;

    @BeforeAll
    static void setUpAll() {
        testDate = LocalDate.now();
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

        // 建立測試資料
        testSnapshot = DailySnapshot.builder()
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
                                .build(),
                        Holding.builder()
                                .stockCode("2317")
                                .stockName("鴻海")
                                .shares(300000L)
                                .weight(new BigDecimal("10.20"))
                                .build()))
                .totalCount(3)
                .totalWeight(new BigDecimal("51.00"))
                .build();

        excelStorageService.saveSnapshot(testSnapshot);
    }

    @Test
    @Order(1)
    @DisplayName("GET /api/holdings/{date} - 查詢指定日期的持倉資料")
    void getHoldingsByDate_ShouldReturnData() throws Exception {
        mockMvc.perform(get("/api/holdings/" + testDate))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.date").value(testDate.toString()))
                .andExpect(jsonPath("$.data.totalCount").value(3))
                .andExpect(jsonPath("$.data.holdings").isArray())
                .andExpect(jsonPath("$.data.holdings.length()").value(3));
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/holdings/{date} - 查詢不存在的日期應回傳 404")
    void getHoldingsByDate_WhenNotExists_ShouldReturn404() throws Exception {
        LocalDate nonExistDate = LocalDate.of(2020, 1, 1);

        mockMvc.perform(get("/api/holdings/" + nonExistDate))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/holdings/latest - 查詢最新持倉資料")
    void getLatestHoldings_ShouldReturnLatestData() throws Exception {
        mockMvc.perform(get("/api/holdings/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.date").value(testDate.toString()))
                .andExpect(jsonPath("$.data.totalCount").value(3));
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/holdings/dates - 查詢可用日期")
    void getAvailableDates_ShouldReturnDates() throws Exception {
        mockMvc.perform(get("/api/holdings/dates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.availableDates").isArray())
                .andExpect(jsonPath("$.data.availableDates.length()").value(1))
                .andExpect(jsonPath("$.data.latestDate").value(testDate.toString()))
                .andExpect(jsonPath("$.data.totalDays").value(1));
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/holdings/search - 依關鍵字搜尋（股票代號）")
    void searchHoldings_ByCode_ShouldReturnMatchingData() throws Exception {
        mockMvc.perform(get("/api/holdings/search")
                .param("keyword", "2330"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].stockCode").value("2330"))
                .andExpect(jsonPath("$.data[0].stockName").value("台積電"));
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/holdings/search - 依關鍵字搜尋（股票名稱）")
    void searchHoldings_ByName_ShouldReturnMatchingData() throws Exception {
        mockMvc.perform(get("/api/holdings/search")
                .param("keyword", "聯發"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].stockCode").value("2454"));
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/holdings/search - 無關鍵字應回傳所有資料")
    void searchHoldings_WithoutKeyword_ShouldReturnAllData() throws Exception {
        mockMvc.perform(get("/api/holdings/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    @Order(8)
    @DisplayName("GET /api/holdings/sorted - 依權重排序（降序）")
    void getSortedHoldings_ByWeightDesc_ShouldReturnSortedData() throws Exception {
        mockMvc.perform(get("/api/holdings/sorted")
                .param("sortBy", "weight")
                .param("ascending", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].stockCode").value("2330")) // 權重最高
                .andExpect(jsonPath("$.data[2].stockCode").value("2317")); // 權重最低
    }

    @Test
    @Order(9)
    @DisplayName("GET /api/holdings/sorted - 依權重排序（升序）")
    void getSortedHoldings_ByWeightAsc_ShouldReturnSortedData() throws Exception {
        mockMvc.perform(get("/api/holdings/sorted")
                .param("sortBy", "weight")
                .param("ascending", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].stockCode").value("2317")) // 權重最低
                .andExpect(jsonPath("$.data[2].stockCode").value("2330")); // 權重最高
    }

    @Test
    @Order(10)
    @DisplayName("GET /api/holdings/sorted - 依股票代號排序")
    void getSortedHoldings_ByCode_ShouldReturnSortedData() throws Exception {
        mockMvc.perform(get("/api/holdings/sorted")
                .param("sortBy", "code")
                .param("ascending", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].stockCode").value("2317"))
                .andExpect(jsonPath("$.data[1].stockCode").value("2330"))
                .andExpect(jsonPath("$.data[2].stockCode").value("2454"));
    }

    @Test
    @Order(11)
    @DisplayName("GET /api/holdings/page - 取得分頁資料（第一頁）")
    void getHoldingsPage_FirstPage_ShouldReturnPagedData() throws Exception {
        mockMvc.perform(get("/api/holdings/page")
                .param("page", "0")
                .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @Order(12)
    @DisplayName("GET /api/holdings/page - 取得分頁資料（最後一頁）")
    void getHoldingsPage_LastPage_ShouldReturnRemainingData() throws Exception {
        mockMvc.perform(get("/api/holdings/page")
                .param("page", "1")
                .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @Order(13)
    @DisplayName("GET /api/holdings/page - 超出範圍的頁碼應回傳空清單")
    void getHoldingsPage_OutOfRange_ShouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/api/holdings/page")
                .param("page", "100")
                .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @Order(14)
    @DisplayName("GET /api/holdings/statistics - 取得統計資訊")
    void getStatistics_ShouldReturnStats() throws Exception {
        mockMvc.perform(get("/api/holdings/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.date").value(testDate.toString()))
                .andExpect(jsonPath("$.data.totalCount").value(3))
                .andExpect(jsonPath("$.data.totalWeight").value(51.00));
    }

    @Test
    @Order(15)
    @DisplayName("多日期資料查詢測試")
    void multiDateQuery_ShouldReturnCorrectData() throws Exception {
        // 新增第二天的資料
        LocalDate secondDate = testDate.minusDays(1);
        DailySnapshot secondSnapshot = DailySnapshot.builder()
                .date(secondDate)
                .holdings(List.of(
                        Holding.builder()
                                .stockCode("2330")
                                .stockName("台積電")
                                .shares(900000L)
                                .weight(new BigDecimal("24.00"))
                                .build()))
                .totalCount(1)
                .totalWeight(new BigDecimal("24.00"))
                .build();

        excelStorageService.saveSnapshot(secondSnapshot);

        // 驗證可用日期更新
        mockMvc.perform(get("/api/holdings/dates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availableDates.length()").value(2))
                .andExpect(jsonPath("$.data.totalDays").value(2));

        // 驗證最新資料仍為 testDate
        mockMvc.perform(get("/api/holdings/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.date").value(testDate.toString()))
                .andExpect(jsonPath("$.data.totalCount").value(3));

        // 驗證可以查詢第二天的資料
        mockMvc.perform(get("/api/holdings/" + secondDate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.date").value(secondDate.toString()))
                .andExpect(jsonPath("$.data.totalCount").value(1));
    }
}
