# 研究文件: ETF 00981A 每日持倉追蹤系統

**Branch**: `001-etf-holdings-tracker` | **Date**: 2025-11-26  
**Status**: Phase 0 完成

---

## 1. 技術選型決策

### 1.1 Java 版本選擇

**Decision**: Java 21 (LTS)

**Rationale**:
- Java 21 是目前最新的長期支援版本 (LTS)，支援至 2031 年
- 與 Spring Boot 3.x 完全相容
- 支援 Virtual Threads (Project Loom)，可優化 I/O 密集型操作
- jlink/jpackage 工具成熟，適合打包桌面應用程式

**Alternatives Considered**:
- Java 17 LTS: 穩定但缺少新特性，考量到專案長期維護選擇 21
- Java 22+: 非 LTS 版本，不適合生產環境

### 1.2 Spring Boot + JavaFX 整合方案

**Decision**: Spring Boot 3.x + JavaFX 21 整合架構

**Rationale**:
- Spring Boot 提供依賴注入、配置管理、元件掃描等企業級功能
- JavaFX 是現代化的 Java GUI 框架，支援 FXML、CSS 樣式化
- 透過 `javafx-weaver` 或手動整合方式，可在 JavaFX 中使用 Spring beans

**Implementation Approach**:
```java
// 主程式啟動順序
1. Spring ApplicationContext 初始化
2. JavaFX Application 啟動
3. 將 Spring Context 注入 JavaFX Controllers
```

**Alternatives Considered**:
- 純 JavaFX (無 Spring): 缺少依賴注入，程式碼耦合度高
- Swing: 過時技術，UI 現代化困難
- Electron/Web: 需要額外技術棧，增加複雜度

### 1.3 HTTP 客戶端選擇

**Decision**: OkHttp 4.x

**Rationale**:
- 成熟穩定的 HTTP 客戶端函式庫
- 內建連線池、重試、逾時控制
- 輕量級，適合桌面應用程式
- 與 Jsoup 良好配合

**Configuration**:
```java
OkHttpClient client = new OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.SECONDS)
    .retryOnConnectionFailure(true)
    .build();
```

**Alternatives Considered**:
- Spring WebClient: Reactive 模式過於複雜，桌面應用不需要
- Apache HttpClient: 較重量級
- Java 11 HttpClient: 功能較基本

### 1.4 Excel 操作函式庫

**Decision**: Apache POI 5.x (poi-ooxml)

**Rationale**:
- Java 生態系中最成熟的 Excel 操作函式庫
- 支援 .xlsx 格式 (Office Open XML)
- 提供完整的讀寫、格式化功能
- 活躍維護，社群資源豐富

**Usage Pattern**:
```java
// 寫入資料
try (XSSFWorkbook workbook = new XSSFWorkbook();
     FileOutputStream out = new FileOutputStream("holdings.xlsx")) {
    XSSFSheet sheet = workbook.createSheet("Holdings");
    // ... 寫入資料
    workbook.write(out);
}
```

**Alternatives Considered**:
- JExcelApi: 僅支援 .xls 格式（舊版）
- Apache Commons CSV: 僅 CSV 格式，不支援 Excel
- EasyExcel (Alibaba): 大量資料優化，此專案資料量小不需要

### 1.5 HTML 解析函式庫

**Decision**: Jsoup 1.17+

**Rationale**:
- Java 中最流行的 HTML 解析函式庫
- 類 jQuery 的 CSS 選擇器語法，易於使用
- 容錯能力強，可處理不規範的 HTML
- 輕量級，無額外依賴

**Usage Pattern**:
```java
Document doc = Jsoup.connect(url)
    .userAgent("Mozilla/5.0...")
    .timeout(10000)
    .get();
Elements rows = doc.select("table.holdings-table tr");
```

**Alternatives Considered**:
- HtmlUnit: 過於重量級，此專案不需要 JavaScript 執行
- Selenium: 需要瀏覽器驅動，部署複雜

---

## 2. 資料來源分析

### 2.1 ezmoney.com.tw 網站結構

**Target URL**: `https://www.ezmoney.com.tw/ETFData/Holdings/00981A`（需確認實際 URL）

**資料抓取策略**:
1. **優先嘗試 API**: 檢查網站是否有 REST API 端點
2. **Fallback DOM 解析**: 若無 API，使用 Jsoup 解析 HTML 表格

**預期資料欄位**:
| 欄位 | 類型 | 說明 |
|------|------|------|
| stockCode | String | 股票代號 (唯一識別) |
| stockName | String | 股票名稱 |
| shares | Long | 持股股數 |
| weight | BigDecimal | 持股權重 (%) |

**注意事項**:
- 需設定適當的 User-Agent 標頭
- 考慮網站防爬蟲機制
- 實作請求頻率限制

### 2.2 資料抓取重試策略

**Decision**: 指數退避重試

**Configuration**:
```java
int maxRetries = 3;
int[] delaySeconds = {2, 4, 8}; // 指數退避
```

**流程**:
1. 首次請求
2. 若失敗，等待 2 秒後重試
3. 若再失敗，等待 4 秒後重試
4. 若仍失敗，等待 8 秒後最後一次重試
5. 全部失敗則顯示錯誤訊息給使用者

---

## 3. 儲存策略

### 3.1 Excel 檔案結構設計

**File Path**: `./data/holdings.xlsx`

**Sheet 結構**:
- 每日資料存儲於以日期命名的 Sheet（如 `2025-11-26`）
- 或使用單一 Sheet，每筆資料包含日期欄位

**Decision**: 單一 Sheet 設計（`Holdings`）

**Schema**:
| Column | Name | Type | Description |
|--------|------|------|-------------|
| A | date | Date | 資料日期 (yyyy-MM-dd) |
| B | stockCode | String | 股票代號 |
| C | stockName | String | 股票名稱 |
| D | shares | Number | 持股股數 |
| E | weight | Number | 持股權重 (%) |

**Rationale**:
- 單一 Sheet 便於查詢和資料管理
- 使用日期欄位可輕鬆過濾特定日期或區間
- 簡化 90 天資料清理邏輯

### 3.2 檔案鎖定處理

**Problem**: Excel 檔案可能被其他程式（如 Microsoft Excel）開啟佔用

**Solution**:
1. 寫入前檢查檔案是否可寫入
2. 使用 try-with-resources 確保檔案正確關閉
3. 若檔案被佔用，顯示友善錯誤訊息並提示使用者關閉其他程式

```java
if (!file.canWrite()) {
    throw new StorageException("檔案被其他程式佔用，請關閉後再試");
}
```

---

## 4. 打包與部署

### 4.1 jlink + jpackage 打包流程

**Decision**: 使用 Maven 插件進行自動化打包

**Plugins**:
- `javafx-maven-plugin`: 處理 JavaFX 模組
- `jlink-jpackage-maven-plugin`: 產生原生安裝檔

**輸出格式**:
- Windows: `.exe` (可攜式執行檔) 或 `.msi` (安裝程式)

**目錄結構** (打包後):
```
etf-holdings-tracker/
├── etf-tracker.exe          # 主程式
├── runtime/                  # 自帶 JRE
├── data/                     # 資料目錄
│   └── holdings.xlsx
└── logs/                     # 日誌目錄
    └── app.log
```

### 4.2 模組化考量

**Java Module System**:
- 需定義 `module-info.java`
- 明確聲明依賴的模組和開放的套件

```java
module com.etf.tracker {
    requires javafx.controls;
    requires javafx.fxml;
    requires spring.boot;
    requires spring.context;
    requires org.apache.poi.ooxml;
    requires jsoup;
    requires okhttp3;
    
    opens com.etf.tracker.fx to javafx.fxml;
    exports com.etf.tracker;
}
```

---

## 5. GUI 設計模式

### 5.1 MVVM 架構

**Decision**: 採用簡化的 MVVM 模式

**Structure**:
- **Model**: Spring Service 層 (業務邏輯)
- **View**: FXML 佈局檔案
- **ViewModel**: JavaFX Controller (處理 UI 狀態與事件)

**Binding**:
```java
// 使用 JavaFX Properties 進行資料綁定
private final ObjectProperty<ObservableList<HoldingDto>> holdings = 
    new SimpleObjectProperty<>(FXCollections.observableArrayList());

// UI 元件綁定
tableView.itemsProperty().bind(holdings);
```

### 5.2 非同步操作處理

**Decision**: 使用 JavaFX Task 處理長時間操作

**Pattern**:
```java
Task<List<Holding>> fetchTask = new Task<>() {
    @Override
    protected List<Holding> call() {
        updateMessage("正在連線...");
        // 執行抓取
        updateMessage("正在解析資料...");
        return holdings;
    }
};

progressIndicator.visibleProperty().bind(fetchTask.runningProperty());
updateButton.disableProperty().bind(fetchTask.runningProperty());
fetchTask.setOnSucceeded(e -> displayData(fetchTask.getValue()));
fetchTask.setOnFailed(e -> showError(fetchTask.getException()));

new Thread(fetchTask).start();
```

---

## 6. 手動 DTO 映射策略

### 6.1 映射器設計

**Decision**: 使用靜態工廠方法進行 POJO 轉換

**不使用映射框架的原因**:
- 專案規模小，實體數量有限
- 手動映射可完全控制轉換邏輯
- 避免額外依賴和編譯時處理

**Implementation Pattern**:
```java
public class HoldingMapper {
    
    public static HoldingDto toDto(Holding entity) {
        if (entity == null) return null;
        return new HoldingDto(
            entity.getStockCode(),
            entity.getStockName(),
            entity.getShares(),
            entity.getWeight()
        );
    }
    
    public static Holding toEntity(HoldingDto dto) {
        if (dto == null) return null;
        return new Holding(
            dto.stockCode(),
            dto.stockName(),
            dto.shares(),
            dto.weight()
        );
    }
    
    public static List<HoldingDto> toDtoList(List<Holding> entities) {
        if (entities == null) return List.of();
        return entities.stream()
            .map(HoldingMapper::toDto)
            .collect(Collectors.toList());
    }
}
```

---

## 7. 日誌策略

### 7.1 Logback 配置

**Log Files**:
- 路徑: `./logs/app.log`
- 輪替: 每日輪替，保留 7 天
- 格式: JSON 結構化日誌

**Configuration** (logback-spring.xml):
```xml
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>./logs/app.log</file>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>./logs/app.%d{yyyy-MM-dd}.log</fileNamePattern>
        <maxHistory>7</maxHistory>
    </rollingPolicy>
</appender>
```

### 7.2 使用者友善訊息

**UI 訊息與技術日誌分離**:
```java
// 技術日誌 (寫入檔案)
log.error("Failed to fetch data from {}: {}", url, exception.getMessage(), exception);

// 使用者訊息 (顯示於 UI)
showUserMessage("無法取得資料，請檢查網路連線後再試");
```

---

## 8. 效能優化考量

### 8.1 Excel 讀取優化

**Approach**: 使用 SAX 事件模式讀取大檔案

```java
// 對於 90 天資料 (~9000 筆記錄)，標準 DOM 模式即可
// 若資料量增長，可改用 SXSSF (streaming) 或 SAX 模式
```

### 8.2 UI 回應性

**Guidelines**:
- 所有網路/檔案 I/O 操作在背景執行緒執行
- 使用 Platform.runLater() 更新 UI
- 避免在 JavaFX Application Thread 執行阻塞操作

---

## 9. 測試策略

### 9.1 單元測試

**Coverage Target**: 80%

**Focus Areas**:
- Service 層業務邏輯
- DTO 映射器
- 資料計算邏輯

**Mocking Strategy**:
- 使用 Mockito mock 外部依賴
- 使用測試用 Excel 檔案進行 ExcelStorageService 測試

### 9.2 整合測試

**Scope**:
- Controller → Service → Storage 完整流程
- GUI 基本操作 (使用 TestFX)

### 9.3 契約測試

**For External Integration**:
- 驗證 ezmoney.com.tw 網頁結構未變更
- 定期執行確保擷取邏輯仍有效

---

## 10. 研究結論

所有技術選型已確定，無 NEEDS CLARIFICATION 項目。專案可進入 Phase 1 設計階段。

**技術棧總結**:
| 層級 | 技術選擇 |
|------|----------|
| 語言 | Java 21 LTS |
| 框架 | Spring Boot 3.x + JavaFX 21 |
| 建置工具 | Maven 3.9+ |
| HTTP 客戶端 | OkHttp 4.x |
| HTML 解析 | Jsoup 1.17+ |
| Excel 操作 | Apache POI 5.x |
| 測試框架 | JUnit 5 + Mockito + TestFX |
| 日誌框架 | Logback + Logstash Encoder |
| 打包工具 | jlink + jpackage (Maven 插件) |

**不使用的技術** (依使用者要求):
- ❌ 資料庫 (改用 Excel 檔案)
- ❌ Redis
- ❌ MapStruct/ModelMapper (改用手動映射)
- ❌ RouterFunctions (改用 @RestController)
