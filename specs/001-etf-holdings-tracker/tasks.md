# Tasks: ETF 00981A 每日持倉追蹤系統

**Input**: Design documents from `/specs/001-etf-holdings-tracker/`  
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅

**Tests**: 本專案將包含單元測試與整合測試，依 Constitution 要求達 80% 覆蓋率

**Organization**: 任務依 User Story 分組，支援獨立實作與測試

---

## 格式說明: `[ID] [P?] [Story?] Description`

- **[P]**: 可平行執行（不同檔案、無相依性）
- **[Story]**: 所屬 User Story（如 US1, US2, US3, US4, US5）
- 描述包含確切檔案路徑

---

## Phase 1: Setup (專案初始化)

**目的**: 專案結構建立與基礎配置

- [X] T001 建立 Maven 專案結構，設定 pom.xml 包含 Spring Boot 3.x, JavaFX 21, Apache POI, Jsoup, OkHttp 依賴，檔案: `pom.xml`
- [X] T002 [P] 建立 Spring Boot 主程式入口，檔案: `src/main/java/com/etf/tracker/EtfHoldingsTrackerApplication.java`
- [X] T003 [P] 建立 application.yml 配置檔，包含資料路徑、網頁擷取、日誌等設定，檔案: `src/main/resources/application.yml`
- [X] T004 [P] 建立 logback-spring.xml 日誌配置（JSON 結構化日誌），檔案: `src/main/resources/logback-spring.xml`
- [X] T005 [P] 建立 module-info.java 模組定義（支援 jlink 打包），檔案: `src/main/java/module-info.java`
- [X] T006 [P] 建立 .gitignore 忽略建置產物與資料檔案，檔案: `.gitignore`

---

## Phase 2: Foundational (基礎架構)

**目的**: 所有 User Story 共用的核心基礎設施

**⚠️ 重要**: 此階段必須完成後，才能開始任何 User Story 實作

### 核心模型

- [X] T007 [P] 建立 Holding 成分股實體類別，檔案: `src/main/java/com/etf/tracker/model/Holding.java`
- [X] T008 [P] 建立 DailySnapshot 每日快照實體類別，檔案: `src/main/java/com/etf/tracker/model/DailySnapshot.java`
- [X] T009 [P] 建立 HoldingChange 持倉變化實體類別，檔案: `src/main/java/com/etf/tracker/model/HoldingChange.java`
- [X] T010 [P] 建立 ChangeType 變化類型列舉，檔案: `src/main/java/com/etf/tracker/model/ChangeType.java`

### DTO 與手動映射器

- [X] T011 [P] 建立 HoldingDto 資料傳輸物件 (Java Record)，檔案: `src/main/java/com/etf/tracker/dto/HoldingDto.java`
- [X] T012 [P] 建立 DailySnapshotDto 資料傳輸物件，檔案: `src/main/java/com/etf/tracker/dto/DailySnapshotDto.java`
- [X] T013 [P] 建立 HoldingChangeDto 資料傳輸物件，檔案: `src/main/java/com/etf/tracker/dto/HoldingChangeDto.java`
- [X] T014 [P] 建立 RangeCompareResultDto 區間比較結果 DTO，檔案: `src/main/java/com/etf/tracker/dto/RangeCompareResultDto.java`
- [X] T015 [P] 建立 ApiResponse 通用回應格式，檔案: `src/main/java/com/etf/tracker/dto/ApiResponse.java`
- [X] T016 [P] 建立 AvailableDatesDto 可查詢日期 DTO，檔案: `src/main/java/com/etf/tracker/dto/AvailableDatesDto.java`
- [X] T017 [P] 建立 CleanupResultDto 清理結果 DTO，檔案: `src/main/java/com/etf/tracker/dto/CleanupResultDto.java`
- [X] T018 [P] 建立 SystemHealthDto 系統健康狀態 DTO，檔案: `src/main/java/com/etf/tracker/dto/SystemHealthDto.java`
- [X] T019 建立 HoldingMapper 手動映射器（Entity ↔ DTO 轉換），檔案: `src/main/java/com/etf/tracker/dto/mapper/HoldingMapper.java`
- [X] T020 建立 DailySnapshotMapper 手動映射器，檔案: `src/main/java/com/etf/tracker/dto/mapper/DailySnapshotMapper.java`
- [X] T021 建立 HoldingChangeMapper 手動映射器，檔案: `src/main/java/com/etf/tracker/dto/mapper/HoldingChangeMapper.java`

### 例外處理

- [X] T022 [P] 建立 DataFetchException 資料抓取例外，檔案: `src/main/java/com/etf/tracker/exception/DataFetchException.java`
- [X] T023 [P] 建立 StorageException 儲存例外，檔案: `src/main/java/com/etf/tracker/exception/StorageException.java`
- [X] T024 [P] 建立 ValidationException 驗證例外，檔案: `src/main/java/com/etf/tracker/exception/ValidationException.java`
- [X] T025 建立 GlobalExceptionHandler 全域例外處理器，檔案: `src/main/java/com/etf/tracker/exception/GlobalExceptionHandler.java`

### 配置類別

- [X] T026 [P] 建立 AppConfig 應用程式配置類別（讀取 application.yml），檔案: `src/main/java/com/etf/tracker/config/AppConfig.java`
- [X] T027 [P] 建立 HttpClientConfig HTTP 客戶端配置（OkHttp 設定），檔案: `src/main/java/com/etf/tracker/config/HttpClientConfiguration.java`

### 測試基礎設施

- [X] T028 [P] 建立測試配置 application-test.yml，檔案: `src/test/resources/application-test.yml`
- [X] T029 [P] 建立測試用範例 Excel 產生器與模擬 HTML，檔案: `src/test/java/com/etf/tracker/test/TestExcelGenerator.java`, `src/test/java/com/etf/tracker/test/MockHtmlData.java`

**Checkpoint**: ✅ 基礎架構就緒 - 可開始 User Story 實作

---

## Phase 3: User Story 1 - 更新並抓取最新持倉資料 (Priority: P1) 🎯 MVP

**Goal**: 使用者點擊「更新」按鈕，系統從網站抓取 ETF 00981A 成分股資料並儲存至 Excel

**Independent Test**: 點擊更新按鈕後，確認 Excel 檔案中新增當日持倉資料

### 測試 (User Story 1)

- [X] T030 [P] [US1] 建立 RetryableWebClient 單元測試，檔案: `src/test/java/com/etf/tracker/scraper/RetryableWebClientTest.java`
- [X] T031 [P] [US1] 建立 EzMoneyScraperStrategy 單元測試（使用模擬 HTML），檔案: `src/test/java/com/etf/tracker/scraper/EzMoneyScraperStrategyTest.java`
- [X] T032 [P] [US1] 建立 DataFetchService 單元測試，檔案: `src/test/java/com/etf/tracker/service/DataFetchServiceTest.java`
- [X] T033 [P] [US1] 建立 ExcelStorageService 單元測試，檔案: `src/test/java/com/etf/tracker/service/ExcelStorageServiceTest.java`

### 實作 (User Story 1)

- [X] T034 [US1] 實作 RetryableWebClient 可重試 HTTP 客戶端（OkHttp + 指數退避），檔案: `src/main/java/com/etf/tracker/scraper/RetryableWebClient.java`
- [X] T035 [US1] 實作 EzMoneyScraperStrategy 網頁擷取策略（Jsoup DOM 解析），檔案: `src/main/java/com/etf/tracker/scraper/EzMoneyScraperStrategy.java`
- [X] T036 [US1] 實作 DataFetchService 資料抓取服務（整合 WebClient + Scraper），檔案: `src/main/java/com/etf/tracker/service/DataFetchService.java`
- [X] T037 [US1] 實作 ExcelStorageService Excel 儲存服務（Apache POI 讀寫），檔案: `src/main/java/com/etf/tracker/service/ExcelStorageService.java`
- [X] T038 [US1] 實作 HoldingController fetch 端點（POST /api/holdings/fetch），檔案: `src/main/java/com/etf/tracker/controller/HoldingController.java`

### GUI (User Story 1)

- [X] T039 [P] [US1] 建立 MainApp JavaFX 主程式（整合 Spring Context），檔案: `src/main/java/com/etf/tracker/gui/MainApp.java`
- [X] T040 [P] [US1] 建立 main.fxml 主視窗佈局，檔案: `src/main/resources/fxml/main.fxml`
- [X] T041 [US1] 建立 MainViewController 主視圖控制器（更新按鈕、進度條、狀態文字），檔案: `src/main/java/com/etf/tracker/gui/MainViewController.java`
- [X] T042 [US1] 建立 LoadingIndicator 載入指示器元件，檔案: `src/main/java/com/etf/tracker/gui/LoadingIndicator.java`

### 整合測試 (User Story 1)

- [X] T043 [US1] 建立 HoldingController 整合測試（抓取與儲存流程），檔案: `src/test/java/com/etf/tracker/controller/HoldingControllerFetchIT.java`

**Checkpoint**: User Story 1 完成 - 可獨立測試資料抓取與儲存功能

---

## Phase 4: User Story 2 - 查看單日持倉資料 (Priority: P1) 🎯 MVP

**Goal**: 使用者可查看特定日期的持倉資料，預設顯示最新資料

**Independent Test**: 選擇不同日期，確認顯示對應日期的持倉資料

### 測試 (User Story 2)

- [X] T044 [P] [US2] 建立 HoldingQueryService 單元測試，檔案: `src/test/java/com/etf/tracker/service/HoldingQueryServiceTest.java`

### 實作 (User Story 2)

- [X] T045 [US2] 實作 HoldingQueryService 持倉查詢服務（單日查詢、最新資料、可用日期），檔案: `src/main/java/com/etf/tracker/service/HoldingQueryService.java`
- [X] T046 [US2] 擴充 HoldingController 查詢端點（GET /api/holdings/{date}, /latest, /available-dates），檔案: `src/main/java/com/etf/tracker/controller/HoldingController.java`

### GUI (User Story 2)

- [X] T047 [P] [US2] 建立 single-day.fxml 單日查詢佈局，檔案: `src/main/resources/fxml/single-day.fxml`
- [X] T048 [US2] 建立 SingleDayViewController 單日查詢視圖控制器，檔案: `src/main/java/com/etf/tracker/gui/view/SingleDayViewController.java`
- [X] T049 [US2] 建立 DatePickerComponent 日期選擇器元件（日曆選單），檔案: `src/main/java/com/etf/tracker/gui/component/DatePickerComponent.java`
- [X] T050 [US2] 建立 HoldingsTableComponent 持倉表格元件（顯示成分股清單），檔案: `src/main/java/com/etf/tracker/gui/component/HoldingsTableComponent.java`

### 整合測試 (User Story 2)

- [X] T051 [US2] 建立 HoldingController 查詢整合測試，檔案: `src/test/java/com/etf/tracker/controller/HoldingControllerQueryIT.java`

**Checkpoint**: User Story 2 完成 - 可獨立測試單日查詢功能

---

## Phase 5: User Story 3 - 區間資料比較與增減計算 (Priority: P2)

**Goal**: 使用者選擇起始與結束日期，系統計算並顯示增減變化（紅增綠減）

**Independent Test**: 選擇兩個有資料的日期，確認正確計算並以顏色區分顯示

### 測試 (User Story 3)

- [X] T052 [P] [US3] 建立 HoldingCompareService 單元測試，檔案: `src/test/java/com/etf/tracker/service/HoldingCompareServiceTest.java`

### 實作 (User Story 3)

- [X] T053 [US3] 實作 HoldingCompareService 持倉比較服務（計算增減股數、變化比例、權重變化），檔案: `src/main/java/com/etf/tracker/service/HoldingCompareService.java`
- [X] T054 [US3] 擴充 HoldingController 比較端點（GET /api/holdings/compare），檔案: `src/main/java/com/etf/tracker/controller/HoldingController.java`

### GUI (User Story 3)

- [X] T055 [P] [US3] 建立 range-compare.fxml 區間比較佈局，檔案: `src/main/resources/fxml/range-compare.fxml`
- [X] T056 [US3] 建立 RangeCompareViewController 區間比較視圖控制器，檔案: `src/main/java/com/etf/tracker/gui/view/RangeCompareViewController.java`
- [X] T057 [US3] 建立 ChangeTableComponent 變化表格元件（紅增綠減顏色樣式），檔案: `src/main/java/com/etf/tracker/gui/component/ChangeTableComponent.java`
- [X] T058 [P] [US3] 擴充 styles.css 應用程式樣式表（增減顏色定義），檔案: `src/main/resources/css/styles.css`

### 整合測試 (User Story 3)

- [X] T059 [US3] 建立 HoldingController 比較整合測試，檔案: `src/test/java/com/etf/tracker/controller/HoldingControllerCompareIT.java`

**Checkpoint**: User Story 3 完成 - 可獨立測試區間比較功能 ✅

---

## Phase 6: User Story 4 - 區間內新進/剔除/增減持整理 (Priority: P2)

**Goal**: 系統自動分類區間內的新進增持、剔除減持、現有增減持

**Independent Test**: 選擇包含新進與剔除股票的日期區間，確認正確分類顯示

> **Note**: Phase 6 的功能已在 Phase 5 實作完成。HoldingCompareService 已包含完整分類邏輯，
> RangeCompareViewController 和 ChangeTableComponent 已實作分類顯示功能。

### 測試 (User Story 4)

- [X] T060 [P] [US4] 擴充 HoldingCompareService 分類測試（測試新進/剔除/增減持分類），檔案: `src/test/java/com/etf/tracker/service/HoldingCompareServiceTest.java`
  - 已在 Phase 5 實作 - 包含 24 個分類測試案例

### 實作 (User Story 4)

- [X] T061 [US4] 擴充 HoldingCompareService 分類邏輯（新進/剔除/增持/減持/不變），檔案: `src/main/java/com/etf/tracker/service/HoldingCompareService.java`
  - 已在 Phase 5 實作 - 完整分類邏輯（NEW_ADDITION, REMOVED, INCREASED, DECREASED, UNCHANGED）
- [X] T062 [US4] 擴充 RangeCompareResultDto 包含分類結果，檔案: `src/main/java/com/etf/tracker/dto/RangeCompareResultDto.java`
  - 已在 Phase 5 實作 - 包含 newAdditions, removals, increased, decreased, unchanged 欄位

### GUI (User Story 4)

- [X] T063 [US4] 擴充 RangeCompareViewController 顯示分類區塊（新進/剔除/增減持），檔案: `src/main/java/com/etf/tracker/gui/view/RangeCompareViewController.java`
  - 已在 Phase 5 實作 - 包含 5 個分類標籤頁和表格
- [X] T064 [US4] 建立 CategoryTabComponent 分類標籤元件（切換顯示各類別），檔案: `src/main/java/com/etf/tracker/gui/component/ChangeTableComponent.java`
  - 已在 Phase 5 以 ChangeTableComponent 實作 - 支援 5 種表格類型和紅增綠減顏色樣式

**Checkpoint**: User Story 4 完成 - 可獨立測試分類整理功能 ✅

---

## Phase 7: User Story 5 - 現代化響應式使用者介面 (Priority: P3)

**Goal**: 應用程式採用現代化視覺風格，支援不同視窗大小

**Independent Test**: 在不同視窗大小下操作，確認介面正常顯示

### GUI 優化 (User Story 5)

- [X] T065 [P] [US5] 優化 styles.css 現代化視覺樣式（先進簡約風格），檔案: `src/main/resources/css/styles.css`
- [X] T066 [US5] 優化 main.fxml 響應式佈局（使用 VBox/HBox/GridPane 彈性配置），檔案: `src/main/resources/fxml/main.fxml`
- [X] T067 [US5] 優化 single-day.fxml 響應式佈局，檔案: `src/main/resources/fxml/single-day.fxml`
- [X] T068 [US5] 優化 range-compare.fxml 響應式佈局，檔案: `src/main/resources/fxml/range-compare.fxml`
- [X] T069 [US5] 實作視窗大小變更監聽器（自動調整版面配置），檔案: `src/main/java/com/etf/tracker/gui/MainApp.java`

### 使用者體驗優化 (User Story 5)

- [X] T070 [US5] 實作導航功能（主畫面、單日查詢、區間比較切換），檔案: `src/main/java/com/etf/tracker/gui/MainViewController.java`
- [X] T071 [US5] 實作鍵盤快捷鍵（F5 更新、Ctrl+Q 離開等），檔案: `src/main/java/com/etf/tracker/gui/MainApp.java`

**Checkpoint**: User Story 5 完成 - UI/UX 優化完成 ✅

---

## Phase 8: 資料清理與系統功能

**目的**: 實作 90 天資料清理與系統健康檢查

### 實作

- [X] T072 [P] 建立 DataCleanupService 資料清理服務單元測試，檔案: `src/test/java/com/etf/tracker/service/DataCleanupServiceTest.java`
- [X] T073 實作 DataCleanupService 資料清理服務（刪除超過 90 天資料），檔案: `src/main/java/com/etf/tracker/service/DataCleanupService.java`
- [X] T074 擴充 HoldingController 清理端點（DELETE /api/holdings/cleanup），檔案: `src/main/java/com/etf/tracker/controller/HoldingController.java`
- [X] T075 建立 SystemController 系統狀態端點（GET /api/system/health），檔案: `src/main/java/com/etf/tracker/controller/SystemController.java`

### GUI

- [X] T076 [P] 在 MainViewController 新增清理按鈕與確認對話框，檔案: `src/main/java/com/etf/tracker/gui/MainViewController.java`
- [X] T077 建立 ConfirmDialog 確認對話框元件，檔案: `src/main/java/com/etf/tracker/gui/component/ConfirmDialog.java`

**Checkpoint**: 資料清理與系統功能完成 ✅

---

## Phase 9: Polish & Cross-Cutting Concerns

**目的**: 打包、文件、最終優化

### 打包與部署

- [X] T078 配置 Maven Assembly Plugin（產生 ZIP 發行包），檔案: `pom.xml`, `src/assembly/distribution.xml`
  - 註：原計劃使用 jlink 但因 Spring Boot 及其依賴 (SLF4J, Hibernate Validator, Jackson, POI) 為非 JPMS 模組化，無法兼容
- [X] T079 配置內嵌 JRE 發行套件（免安裝 Java 即可執行），檔案: `pom.xml`, `src/assembly/distribution-with-jre.xml`
  - 註：原計劃使用 jpackage 產生 Windows .exe/.msi，改為 Maven Antrun + Adoptium API 下載 JRE
- [X] T080 建立應用程式圖示與啟動畫面，檔案: `src/main/resources/images/`

### 文件

- [X] T081 [P] 更新 README.md 專案說明文件，檔案: `README.md`
- [X] T082 [P] 確認所有公開方法有 Javadoc 文件

### 最終驗證

- [X] T083 執行完整測試套件確認覆蓋率 ≥80%
- [X] T084 執行 quickstart.md 驗證檢查清單
- [X] T085 建置最終 Windows 執行檔並進行冒煙測試 (使用 dist profile 建置 ZIP 發行包)

---

## 相依性與執行順序

### 階段相依性

- **Phase 1 (Setup)**: 無相依性 - 可立即開始
- **Phase 2 (Foundational)**: 依賴 Phase 1 完成 - **阻擋所有 User Stories**
- **Phase 3-7 (User Stories)**: 依賴 Phase 2 完成
  - P1 Stories (US1, US2) 可先平行進行
  - P2 Stories (US3, US4) 可在 P1 完成後進行
  - P3 Story (US5) 最後進行
- **Phase 8 (Data Cleanup)**: 依賴 Phase 2 完成
- **Phase 9 (Polish)**: 依賴所有功能完成

### User Story 相依性

| Story | 優先級 | 前置相依 | 可平行執行 |
|-------|--------|----------|------------|
| US1 | P1 | Phase 2 (Foundational) | 是（與 US2） |
| US2 | P1 | Phase 2 (Foundational) | 是（與 US1） |
| US3 | P2 | Phase 2 (Foundational) | 是（與 US4） |
| US4 | P2 | US3 部分完成 | 否（擴充 US3） |
| US5 | P3 | US1-US4 GUI 完成 | 否（優化既有） |

### 平行執行範例

```bash
# Phase 1: 所有標記 [P] 的任務可平行執行
T002, T003, T004, T005, T006

# Phase 2: 模型與 DTO 可平行建立
T007, T008, T009, T010, T011, T012, T013...T018, T022, T023, T024, T026, T027

# Phase 3 (US1): 測試可平行編寫
T030, T031, T032, T033

# Phase 4 (US2): 可與 Phase 3 平行進行
T044 (測試) + T045 (實作)

# Phase 5 (US3) + Phase 6 (US4): 可平行進行
T052, T060 (測試)
```

---

## 實施策略

### MVP 優先 (User Story 1 + 2)

1. 完成 Phase 1: Setup
2. 完成 Phase 2: Foundational (**關鍵 - 阻擋所有 Stories**)
3. 完成 Phase 3: User Story 1 (資料抓取)
4. 完成 Phase 4: User Story 2 (單日查詢)
5. **停止並驗證**: 獨立測試 MVP 功能
6. 可部署/展示

### 增量交付

1. Setup + Foundational → 基礎就緒
2. 新增 User Story 1 → 獨立測試 → 部署/展示 (MVP!)
3. 新增 User Story 2 → 獨立測試 → 部署/展示
4. 新增 User Story 3 + 4 → 獨立測試 → 部署/展示
5. 新增 User Story 5 → 最終優化 → 正式發佈

---

## 任務統計摘要

| 階段 | 任務數 | 可平行 |
|------|--------|--------|
| Phase 1: Setup | 6 | 5 |
| Phase 2: Foundational | 23 | 18 |
| Phase 3: US1 (P1) | 14 | 7 |
| Phase 4: US2 (P1) | 8 | 2 |
| Phase 5: US3 (P2) | 8 | 3 |
| Phase 6: US4 (P2) | 5 | 1 |
| Phase 7: US5 (P3) | 7 | 1 |
| Phase 8: Data Cleanup | 6 | 2 |
| Phase 9: Polish | 8 | 2 |
| **總計** | **85** | **41** |

---

## 備註

- 標記 [P] 的任務可平行執行（不同檔案、無相依性）
- 標記 [Story] 的任務可追溯到對應的 User Story
- 每個 User Story 應可獨立完成並測試
- 驗證測試失敗後再實作
- 每個任務或邏輯群組完成後進行 commit
- 可在任何 Checkpoint 停止以獨立驗證功能
- 避免: 模糊任務、同檔案衝突、破壞獨立性的跨 Story 相依性
