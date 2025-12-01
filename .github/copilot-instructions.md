# ETF Holdings Tracker - Copilot 指南

## 專案概述

這是一個用於追蹤 ETF 00981A 每日持倉變化的 **Windows 桌面應用程式**，同時支援 REST API 網頁版。

**技術堆疊**: Java 21 + Spring Boot 3.2 + JavaFX 21 + Apache POI + Jsoup + Playwright

## 架構概念

```
┌─────────────────────────────────────────────────────────────┐
│ GUI (JavaFX)          │  REST API (Spring MVC)              │
│ MainApp/Controller    │  HoldingController                  │
├───────────────────────┴─────────────────────────────────────┤
│                    Service Layer                            │
│  DataFetchService │ HoldingQueryService │ ExcelStorageService │
├─────────────────────────────────────────────────────────────┤
│                    Scraper Layer                            │
│  PlaywrightWebClient → EzMoneyScraperStrategy (Jsoup 解析)  │
├─────────────────────────────────────────────────────────────┤
│                    Storage (Excel)                          │
│  ./data/holdings.xlsx - 透過 Apache POI 讀寫                │
└─────────────────────────────────────────────────────────────┘
```

## 核心資料流

1. **資料抓取**: `DataFetchService` → `PlaywrightWebClient.fetchHtml()` → `EzMoneyScraperStrategy.parseHoldings()`
2. **資料模型**: `DailySnapshot` (日期快照) 包含多個 `Holding` (個股持倉)
3. **持倉比較**: `HoldingCompareService.compareRange()` 計算新進/剔除/增持/減持

## 開發指令

```bash
# 桌面版 (JavaFX GUI)
mvn javafx:run

# 網頁版 (REST API, port 8080)
mvn spring-boot:run

# 執行測試 + 覆蓋率報告
mvn test jacoco:report

# 建置發行套件 (含 JRE，免安裝 Java)
mvn package -Pdist-jre-prepare -DskipTests  # 步驟 1: 下載 JRE
mvn package -Pdist-jre -DskipTests          # 步驟 2: 打包
```

## 專案慣例

### 目錄結構

| 路徑 | 說明 |
|------|------|
| `controller/` | REST 端點，對應 `/api/holdings/**` |
| `service/` | 業務邏輯層，每個功能一個 Service |
| `scraper/` | 網頁擷取策略，使用 Playwright + Jsoup |
| `dto/` | API 回應物件，搭配 `dto/mapper/` 轉換 |
| `model/` | 領域模型 (`DailySnapshot`, `Holding`) |
| `exception/` | 自訂例外，使用工廠方法建立 (如 `DataFetchException.timeout()`) |
| `gui/` | JavaFX 控制器與元件 |

### 命名規範

- **整合測試**: `*IT.java` (如 `HoldingControllerCompareIT.java`)
- **單元測試**: `*Test.java`，使用 `@Nested` 分組測試案例
- **DTO 轉換**: 透過 `dto/mapper/*Mapper.java` 靜態方法

### 配置結構 (`application.yml`)

```yaml
app:
  data:
    storage-path: ./data         # Excel 儲存位置
    retention-days: 90           # 資料保留天數
  scraper:
    target-url: https://...      # 目標網站
    max-retries: 3               # 重試次數
    retry-delays: [2, 4, 8]      # 指數退避
```

### 測試配置

- 測試使用 `@ActiveProfiles("test")` 啟用 `application-test.yml`
- 測試資料寫入 `./target/test-data/`
- MockMvc 測試 REST API，Mockito 測試 Service 層

## API 端點參考

| 方法 | 端點 | 說明 |
|------|------|------|
| POST | `/api/holdings/fetch` | 抓取最新持倉 |
| GET | `/api/holdings/query/{date}` | 單日查詢 |
| GET | `/api/holdings/compare?startDate=&endDate=` | 區間比較 |
| DELETE | `/api/holdings/cleanup` | 清理舊資料 |

## 例外處理模式

使用工廠方法建立例外，保留完整上下文：

```java
// ✅ 正確
throw DataFetchException.timeout(url);
throw DataFetchException.httpError(url, 500);
throw DataFetchException.parseError(content, cause);

// ❌ 避免
throw new DataFetchException("連線失敗");
```

## 規格文件

詳細需求規格位於 `specs/001-etf-holdings-tracker/`:
- `spec.md` - 功能規格與 User Stories
- `plan.md` - 實作計畫
- `tasks.md` - 任務清單

## Prompt 範本

使用 `.github/prompts/` 中的 slash command 範本進行開發協作。
