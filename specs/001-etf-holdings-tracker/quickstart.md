# 快速入門指南: ETF 00981A 每日持倉追蹤系統

**Branch**: `001-etf-holdings-tracker` | **Date**: 2025-11-26  
**Status**: Phase 1 設計

---

## 環境需求

### 開發環境

| 項目 | 版本 | 說明 |
|------|------|------|
| JDK | 21 (LTS) | 建議使用 Eclipse Temurin 或 Amazon Corretto |
| Maven | 3.9+ | 建置工具 |
| IDE | IntelliJ IDEA / VS Code | 需安裝 Java 擴充套件 |
| Git | 2.x | 版本控制 |

### 執行環境 (使用者)

| 項目 | 需求 |
|------|------|
| 作業系統 | Windows 10/11 (x64) |
| 網路連線 | 需要 (用於抓取 ETF 資料) |
| Java | 不需要 (應用程式自帶 JRE) |

---

## 專案設定

### 1. Clone 專案

```bash
git clone https://github.com/HeimlichLin/etf-holdings-tracker.git
cd etf-holdings-tracker
git checkout 001-etf-holdings-tracker
```

### 2. 確認 JDK 版本

```bash
java -version
# 應顯示: openjdk version "21.x.x"
```

### 3. 建置專案

```bash
mvn clean install
```

---

## 專案結構總覽

```
etf-holdings-tracker/
├── pom.xml                          # Maven 設定檔
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/etf/tracker/
│   │   │       ├── EtfHoldingsTrackerApplication.java
│   │   │       ├── config/
│   │   │       ├── model/
│   │   │       ├── dto/
│   │   │       ├── service/
│   │   │       ├── controller/
│   │   │       ├── scraper/
│   │   │       ├── exception/
│   │   │       └── fx/
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── logback-spring.xml
│   │       └── fxml/
│   └── test/
├── specs/
│   └── 001-etf-holdings-tracker/
│       ├── spec.md
│       ├── plan.md
│       ├── research.md
│       ├── data-model.md
│       ├── quickstart.md
│       └── contracts/
└── README.md
```

---

## 核心依賴說明

### Maven pom.xml 關鍵依賴

```xml
<properties>
    <java.version>21</java.version>
    <javafx.version>21</javafx.version>
    <spring-boot.version>3.2.0</spring-boot.version>
</properties>

<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- JavaFX -->
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>${javafx.version}</version>
    </dependency>
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-fxml</artifactId>
        <version>${javafx.version}</version>
    </dependency>
    
    <!-- Excel 操作 -->
    <dependency>
        <groupId>org.apache.poi</groupId>
        <artifactId>poi-ooxml</artifactId>
        <version>5.2.5</version>
    </dependency>
    
    <!-- HTML 解析 -->
    <dependency>
        <groupId>org.jsoup</groupId>
        <artifactId>jsoup</artifactId>
        <version>1.17.2</version>
    </dependency>
    
    <!-- HTTP 客戶端 -->
    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>okhttp</artifactId>
        <version>4.12.0</version>
    </dependency>
    
    <!-- 測試 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testfx</groupId>
        <artifactId>testfx-junit5</artifactId>
        <version>4.0.18</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 開發工作流程

### 日常開發

```bash
# 1. 執行單元測試
mvn test

# 2. 執行應用程式 (開發模式)
mvn javafx:run

# 3. 產生測試覆蓋率報告
mvn jacoco:report
```

### 建置發佈版本

```bash
# 完整建置 (含測試)
mvn clean package

# 產生 Windows 執行檔
mvn javafx:jlink jpackage:jpackage
```

---

## 設定檔說明

### application.yml

```yaml
# 應用程式設定
app:
  data:
    # 資料儲存路徑 (相對於執行檔目錄)
    storage-path: ./data
    # Excel 檔案名稱
    file-name: holdings.xlsx
    # 保留天數 (統計查詢範圍)
    retention-days: 90
  
  scraper:
    # 目標網站 URL
    target-url: https://www.ezmoney.com.tw/ETFData/Holdings/00981A
    # 請求逾時 (秒)
    timeout: 10
    # 重試次數
    max-retries: 3
    # 重試間隔 (秒)
    retry-delay: [2, 4, 8]

# Spring Boot 設定
server:
  port: 8080  # 內部 API 埠號

# 日誌設定
logging:
  file:
    path: ./logs
    name: app.log
  level:
    com.etf.tracker: DEBUG
    root: INFO
```

### logback-spring.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 控制台輸出 (開發用) -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- 檔案輸出 (JSON 格式) -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>./logs/app.log</file>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>./logs/app.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>7</maxHistory>
        </rollingPolicy>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

---

## 關鍵元件使用範例

### 1. 資料抓取服務

```java
@Service
public class DataFetchService {
    
    @Autowired
    private RetryableWebClient webClient;
    
    @Autowired
    private EzMoneyScraperStrategy scraper;
    
    public DailySnapshot fetchLatestHoldings() {
        String html = webClient.get(targetUrl);
        List<Holding> holdings = scraper.parse(html);
        return new DailySnapshot(LocalDate.now(), holdings);
    }
}
```

### 2. Excel 儲存服務

```java
@Service
public class ExcelStorageService {
    
    public void save(DailySnapshot snapshot) {
        try (Workbook workbook = loadOrCreateWorkbook()) {
            Sheet sheet = getOrCreateSheet(workbook, "Holdings");
            appendData(sheet, snapshot);
            saveWorkbook(workbook);
        }
    }
    
    public Optional<DailySnapshot> load(LocalDate date) {
        try (Workbook workbook = loadWorkbook()) {
            Sheet sheet = workbook.getSheet("Holdings");
            return extractDataByDate(sheet, date);
        }
    }
}
```

### 3. 手動 DTO 映射

```java
// Entity → DTO
Holding entity = new Holding("2330", "台積電", 1234567L, new BigDecimal("12.34"));
HoldingDto dto = HoldingMapper.toDto(entity);

// DTO → Entity
HoldingDto dto = new HoldingDto("2330", "台積電", 1234567L, new BigDecimal("12.34"));
Holding entity = HoldingMapper.toEntity(dto);

// List 轉換
List<Holding> entities = fetchHoldings();
List<HoldingDto> dtos = HoldingMapper.toDtoList(entities);
```

### 4. JavaFX 非同步操作

```java
public class MainViewController {
    
    @FXML private Button updateButton;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label statusLabel;
    
    @FXML
    void onUpdateClick(ActionEvent event) {
        Task<DailySnapshot> task = new Task<>() {
            @Override
            protected DailySnapshot call() {
                updateMessage("正在連線...");
                // 執行資料抓取
                updateMessage("正在解析資料...");
                return dataFetchService.fetchLatestHoldings();
            }
        };
        
        // 綁定 UI 狀態
        progressIndicator.visibleProperty().bind(task.runningProperty());
        updateButton.disableProperty().bind(task.runningProperty());
        statusLabel.textProperty().bind(task.messageProperty());
        
        // 處理結果
        task.setOnSucceeded(e -> displayData(task.getValue()));
        task.setOnFailed(e -> showError(task.getException()));
        
        new Thread(task).start();
    }
}
```

---

## 測試指南

### 執行所有測試

```bash
mvn test
```

### 執行特定測試類別

```bash
mvn test -Dtest=DataFetchServiceTest
```

### 測試覆蓋率報告

```bash
mvn jacoco:report
# 報告位於: target/site/jacoco/index.html
```

### 整合測試

```bash
mvn verify -P integration-tests
```

---

## 常見問題排解

### Q1: JavaFX 無法啟動

**症狀**: `Error: JavaFX runtime components are missing`

**解決方案**:
1. 確認 pom.xml 包含 JavaFX 依賴
2. 使用 `mvn javafx:run` 而非直接執行 main class
3. 若使用 IDE，確認已配置 VM options:
   ```
   --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml
   ```

### Q2: Excel 檔案被鎖定

**症狀**: `java.io.IOException: The process cannot access the file`

**解決方案**:
1. 關閉 Microsoft Excel 或其他開啟檔案的程式
2. 應用程式會顯示提示訊息

### Q3: 網頁抓取失敗

**症狀**: `DataFetchException: 無法取得資料`

**解決方案**:
1. 檢查網路連線
2. 確認目標網站可正常訪問
3. 檢查 `./logs/app.log` 中的詳細錯誤訊息
4. 若網站結構變更，可能需要更新 scraper 邏輯

---

## 發佈檢查清單

- [ ] 所有單元測試通過
- [ ] 測試覆蓋率 ≥ 80%
- [ ] 整合測試通過
- [ ] 程式碼通過靜態分析 (Checkstyle)
- [ ] 應用程式可正常啟動
- [ ] 資料抓取功能正常
- [ ] Excel 儲存/讀取正常
- [ ] 區間比較功能正常
- [ ] Windows 執行檔可正常執行
