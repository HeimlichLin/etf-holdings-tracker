# Implementation Plan: ETF 00981A 每日持倉追蹤系統

**Branch**: `001-etf-holdings-tracker` | **Date**: 2025-11-26 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-etf-holdings-tracker/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

開發一個 Windows 桌面應用程式，用於追蹤 ETF 00981A 的每日持倉變化。系統將從 ezmoney.com.tw 網站抓取成分股資料，使用 Excel 檔案作為本地儲存，提供單日查詢、區間比較及增減分析功能。採用 Java Spring Boot 3.x + JavaFX 架構，透過 jlink + jpackage 打包為自帶 JRE 的 Windows 原生執行檔。

## Technical Context

**Language/Version**: Java 21 (LTS)
**Framework**: Spring Boot 3.x + JavaFX 21 (桌面 GUI)
**Build Tool**: Maven 3.9+
**Primary Dependencies**:
  - Spring Boot 3.x (核心框架、依賴注入、配置管理)
  - JavaFX 21 (GUI 框架)
  - Apache POI 5.x (Excel 檔案讀寫)
  - Jsoup 1.17+ (HTML DOM 解析)
  - OkHttp 4.x / Spring WebClient (HTTP 客戶端)
  - Logback (日誌記錄)
  - Lombok (減少樣板程式碼) - 可選
**Storage**: Excel 檔案 (`./data/holdings.xlsx`)，無資料庫
**Testing**: JUnit 5 + Mockito + TestFX (GUI 測試)
**Target Platform**: Windows 10/11 (x64)，使用 jlink + jpackage 產生 .exe/.msi
**Project Type**: Desktop 單一專案 (JavaFX + Spring Boot 整合)
**DTO Mapping**: Manual Mapping (手動 POJO 轉換，不使用 MapStruct/ModelMapper)
**API Style**: @RestController (標準控制器類別，非 RouterFunctions)
**Performance Goals**:
  - 資料抓取與儲存 <10 秒
  - 單日查詢 <2 秒
  - 區間比較查詢 <3 秒
**Constraints**:
  - 資料抓取失敗自動重試 1-3 次（間隔數秒）
  - 外部 API 呼叫逾時 10 秒
  - 介面支援 320px-1920px 視窗寬度
  - 查詢統計範圍限制 90 天
**Scale/Scope**:
  - 單一使用者桌面應用程式
  - 成分股數量：數十至數百檔
  - 歷史資料：最多 90 天（使用者手動清理）

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### I. Code Quality Standards

| 規則 | 狀態 | 備註 |
|------|------|------|
| 遵循 Java 編碼慣例 | ✅ 符合 | 使用標準 Java/Spring Boot 慣例 |
| 方法不超過 50 行 | ✅ 符合 | 設計時遵守 |
| 類別不超過 500 行 | ✅ 符合 | 設計時遵守 |
| 循環複雜度 ≤10 | ✅ 符合 | 設計時遵守 |
| 公開 API 有 Javadoc | ✅ 符合 | 所有公開方法將有文件 |
| 程式碼去重 (DRY) | ✅ 符合 | 設計時遵守 |
| 靜態分析通過 | ✅ 符合 | 使用 Checkstyle/SpotBugs |

### II. Testing Discipline

| 規則 | 狀態 | 備註 |
|------|------|------|
| 單元測試覆蓋率 ≥80% | ✅ 符合 | JUnit 5 + Mockito |
| 公開 API 有整合測試 | ✅ 符合 | 將實作 |
| 測試獨立可重複 | ✅ 符合 | Mock 外部依賴 |
| 關鍵邏輯有邊界案例 | ✅ 符合 | 設計時涵蓋 |

### III. User Experience Consistency

| 規則 | 狀態 | 備註 |
|------|------|------|
| API 回應格式一致 | ✅ 符合 | 內部服務層統一格式 |
| 錯誤回應含必要欄位 | ✅ 符合 | 包含錯誤碼、訊息、時間戳 |
| 日期時間使用 ISO 8601 | ✅ 符合 | 統一使用 |
| 支援國際化 (i18n) | ✅ 符合 | 繁體中文介面 |

### IV. Performance Requirements

| 規則 | 狀態 | 備註 |
|------|------|------|
| 外部 API 逾時 10 秒 | ✅ 符合 | 設計時配置 |
| 重試邏輯 | ✅ 符合 | 1-3 次重試 |
| 批次操作分頁 | N/A | 單一使用者，資料量小 |

### V. Observability & Reliability

| 規則 | 狀態 | 備註 |
|------|------|------|
| 結構化日誌 (JSON) | ✅ 符合 | Logback 配置 |
| 健康檢查端點 | N/A | 桌面應用程式 |
| 配置外部化 | ✅ 符合 | application.yml |
| 敏感資料不入日誌 | ✅ 符合 | 設計時遵守 |
| 斷路器模式 | ✅ 符合 | 外部 API 呼叫實作 |

### VI. Documentation Language Standards

| 規則 | 狀態 | 備註 |
|------|------|------|
| Constitution 保持英文 | ✅ 符合 | 已遵守 |
| 規格使用繁體中文 | ✅ 符合 | spec.md 已使用 |
| 計畫使用繁體中文 | ✅ 符合 | 此文件 |
| 使用者文件繁體中文 | ✅ 符合 | 將遵守 |
| 程式碼識別符使用英文 | ✅ 符合 | 將遵守 |

**GATE 結果**: ✅ 通過 - 無違規項目

## Project Structure

### Documentation (this feature)

```text
specs/001-etf-holdings-tracker/
├── plan.md              # 此檔案 (/speckit.plan 命令輸出)
├── research.md          # Phase 0 輸出 (/speckit.plan 命令)
├── data-model.md        # Phase 1 輸出 (/speckit.plan 命令)
├── quickstart.md        # Phase 1 輸出 (/speckit.plan 命令)
├── contracts/           # Phase 1 輸出 (/speckit.plan 命令)
│   └── internal-api.md  # 內部服務層 API 定義
├── checklists/          # 檢查清單
│   └── requirements.md  # 需求檢查清單
└── tasks.md             # Phase 2 輸出 (/speckit.tasks 命令)
```

### Source Code (repository root)

```text
src/
├── main/
│   ├── java/
│   │   └── com/etf/tracker/
│   │       ├── EtfHoldingsTrackerApplication.java  # Spring Boot 主程式
│   │       ├── config/                              # 配置類別
│   │       │   ├── AppConfig.java                   # 應用程式配置
│   │       │   └── HttpClientConfig.java            # HTTP 客戶端配置
│   │       ├── model/                               # 領域模型
│   │       │   ├── Holding.java                     # 成分股實體
│   │       │   ├── DailySnapshot.java               # 每日快照實體
│   │       │   └── HoldingChange.java               # 持倉變化實體
│   │       ├── dto/                                 # 資料傳輸物件
│   │       │   ├── HoldingDto.java                  # 成分股 DTO
│   │       │   ├── DailySnapshotDto.java            # 每日快照 DTO
│   │       │   ├── HoldingChangeDto.java            # 持倉變化 DTO
│   │       │   └── mapper/                          # 手動映射器
│   │       │       └── HoldingMapper.java           # 手動 POJO 轉換
│   │       ├── service/                             # 業務邏輯層
│   │       │   ├── DataFetchService.java            # 資料抓取服務
│   │       │   ├── ExcelStorageService.java         # Excel 儲存服務
│   │       │   ├── HoldingQueryService.java         # 持倉查詢服務
│   │       │   └── HoldingCompareService.java       # 持倉比較服務
│   │       ├── controller/                          # REST 控制器
│   │       │   └── HoldingController.java           # 持倉 API 端點
│   │       ├── scraper/                             # 網頁擷取模組
│   │       │   ├── EzMoneyScraperStrategy.java      # ezmoney 擷取策略
│   │       │   └── RetryableWebClient.java          # 可重試 HTTP 客戶端
│   │       ├── exception/                           # 自訂例外
│   │       │   ├── DataFetchException.java          # 資料抓取例外
│   │       │   └── GlobalExceptionHandler.java      # 全域例外處理器
│   │       └── fx/                                  # JavaFX GUI 模組
│   │           ├── MainApp.java                     # JavaFX 主程式
│   │           ├── view/                            # 視圖元件
│   │           │   ├── MainView.java                # 主視圖
│   │           │   ├── SingleDayView.java           # 單日查詢視圖
│   │           │   └── RangeCompareView.java        # 區間比較視圖
│   │           └── component/                       # 共用元件
│   │               ├── LoadingIndicator.java        # 載入指示器
│   │               └── DatePickerComponent.java     # 日期選擇器
│   └── resources/
│       ├── application.yml                          # Spring Boot 配置
│       ├── logback-spring.xml                       # 日誌配置
│       └── fxml/                                    # JavaFX FXML 佈局
│           ├── main.fxml                            # 主視窗佈局
│           ├── single-day.fxml                      # 單日查詢佈局
│           └── range-compare.fxml                   # 區間比較佈局
└── test/
    ├── java/
    │   └── com/etf/tracker/
    │       ├── service/                             # 服務層單元測試
    │       │   ├── DataFetchServiceTest.java
    │       │   ├── ExcelStorageServiceTest.java
    │       │   ├── HoldingQueryServiceTest.java
    │       │   └── HoldingCompareServiceTest.java
    │       ├── scraper/                             # 擷取模組測試
    │       │   └── EzMoneyScraperStrategyTest.java
    │       └── integration/                         # 整合測試
    │           └── HoldingControllerIT.java
    └── resources/
        ├── application-test.yml                     # 測試配置
        └── test-data/                               # 測試資料
            └── sample-holdings.xlsx                 # 範例 Excel 檔案
```

**Structure Decision**: 採用 Single Project 結構，因為此專案是獨立的 Windows 桌面應用程式，
整合 Spring Boot (核心服務) + JavaFX (GUI)，使用標準 Maven 專案結構。

## Complexity Tracking

> **Constitution Check 無違規項目，此區塊保持空白**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| N/A | N/A | N/A |

---

## Constitution Re-Check (Post-Design)

*設計完成後重新評估 Constitution 合規性*

### 設計決策驗證

| 設計決策 | Constitution 原則 | 合規狀態 |
|----------|------------------|----------|
| Spring Boot 3.x + JavaFX 21 架構 | 程式碼品質標準 | ✅ 符合 - 支援模組化設計 |
| 手動 POJO 映射 (無 MapStruct) | 程式碼品質標準 | ✅ 符合 - 簡化依賴，程式碼清晰 |
| @RestController (無 RouterFunctions) | 使用者體驗一致性 | ✅ 符合 - 標準 Spring MVC 模式 |
| Excel 檔案儲存 (無資料庫) | 效能需求 | ✅ 符合 - 資料量小，適用場景 |
| OkHttp + Jsoup 混合策略 | 可靠性需求 | ✅ 符合 - 含重試與逾時機制 |
| JUnit 5 + Mockito 測試框架 | 測試紀律 | ✅ 符合 - 達 80% 覆蓋率目標 |
| JSON 結構化日誌 (Logback) | 可觀測性 | ✅ 符合 - 含 correlation ID |
| jlink + jpackage 打包 | 效能需求 | ✅ 符合 - 自帶 JRE 部署 |

### 文件語言驗證

| 文件 | 預期語言 | 實際語言 | 狀態 |
|------|----------|----------|------|
| constitution.md | English | English | ✅ |
| spec.md | 繁體中文 | 繁體中文 | ✅ |
| plan.md | 繁體中文 | 繁體中文 | ✅ |
| research.md | 繁體中文 | 繁體中文 | ✅ |
| data-model.md | 繁體中文 | 繁體中文 | ✅ |
| quickstart.md | 繁體中文 | 繁體中文 | ✅ |
| contracts/internal-api.md | 繁體中文 | 繁體中文 | ✅ |

### 最終 GATE 結果

**✅ 通過** - 所有設計決策符合 Constitution 原則，無違規項目。

---

## 生成產物總覽

| 產物 | 路徑 | 狀態 |
|------|------|------|
| 實施計畫 | `specs/001-etf-holdings-tracker/plan.md` | ✅ 已完成 |
| 研究文件 | `specs/001-etf-holdings-tracker/research.md` | ✅ 已完成 |
| 資料模型 | `specs/001-etf-holdings-tracker/data-model.md` | ✅ 已完成 |
| API 合約 | `specs/001-etf-holdings-tracker/contracts/internal-api.md` | ✅ 已完成 |
| 快速入門 | `specs/001-etf-holdings-tracker/quickstart.md` | ✅ 已完成 |
| Agent 上下文 | `.github/agents/copilot-instructions.md` | ✅ 已更新 |

---

## 下一步

執行 `/speckit.tasks` 命令進入 Phase 2，生成詳細的實施任務清單。
