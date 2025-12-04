# ETF Holdings Tracker

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-blue.svg)](https://openjfx.io/)
[![Maven](https://img.shields.io/badge/Maven-3.9+-C71A36.svg)](https://maven.apache.org/)
[![Test Coverage](https://img.shields.io/badge/Coverage-80%25+-green.svg)](#🧪-測試)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Windows-0078D6.svg)](#📋-系統需求)

追蹤 ETF 00981A 每日持倉變化的 **Windows 桌面應用程式**，同時支援 REST API 網頁版。

---

## 📖 目錄

- [功能特色](#🎯-功能特色)
- [系統需求](#📋-系統需求)
- [環境安裝設定](#🔧-環境安裝設定)
- [快速開始](#🚀-快速開始)
- [Google Apps Script 版本](#📜-google-apps-script-版本)
- [專案結構](#📁-專案結構)
- [系統架構](#🏗️-系統架構)
- [技術架構](#🛠️-技術架構)
- [API 端點](#🔌-api-端點)
- [使用說明](#📖-使用說明)
- [開發規範](#📏-開發規範)
- [測試](#🧪-測試)
- [配置](#🔧-配置)
- [貢獻指南](#🤝-貢獻指南)
- [文件資源](#📚-文件資源)
- [授權](#📄-授權)

---

## 🎯 功能特色

| 功能 | 說明 |
|------|------|
| 📥 **自動/手動抓取資料** | 每日 16:00 自動排程，或手動從 ezmoney.com.tw 抓取最新成分股資料 |
| 📊 **單日查詢** | 查看特定日期的持倉資料（股票代號、名稱、股數、權重） |
| 📈 **區間比較分析** | 比較兩個日期間的持倉變化（紅增綠減） |
| 🔄 **變化自動分類** | 智慧分類新進增持/剔除減持/增減持股票 |
| 🗂️ **Excel 本地儲存** | 資料儲存為 Excel 格式，可攜式資料管理 |
| ☁️ **Google Sheets 同步** | 可選擇同步至 Google Sheets 雲端儲存 |
| 🧹 **資料清理** | 支援手動清理超過 90 天的舊資料 |
| 📦 **免安裝執行** | 自帶 JRE 的 Windows 原生執行檔，無需預先安裝 Java |
| 📜 **GAS 獨立版本** | Google Apps Script 版本，可直接在 Google Sheets 中運行 |

---

## 📋 系統需求

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
| 網路連線 | 需要 (用於抓取資料) |
| Java | 不需要 (應用程式自帶 JRE) |

## 🔧 環境安裝設定

### 安裝 JDK 21

#### Windows (使用 Winget)

```powershell
# 安裝 Eclipse Temurin JDK 21
winget install EclipseAdoptium.Temurin.21.JDK

# 或安裝 Amazon Corretto JDK 21
winget install Amazon.Corretto.21.JDK

# 驗證安裝
java -version
```

#### Windows (手動安裝)

1. 下載 [Eclipse Temurin JDK 21](https://adoptium.net/temurin/releases/?version=21) 或 [Amazon Corretto 21](https://docs.aws.amazon.com/corretto/latest/corretto-21-ug/downloads-list.html)
2. 執行安裝程式，勾選「設定 JAVA_HOME 變數」
3. 開啟新的命令提示字元，執行 `java -version` 驗證

#### macOS (使用 Homebrew)

```bash
# 安裝 Eclipse Temurin JDK 21
brew install --cask temurin@21

# 驗證安裝
java -version
```

#### Linux (Ubuntu/Debian)

```bash
# 安裝 Eclipse Temurin JDK 21
sudo apt update
sudo apt install -y wget apt-transport-https
wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo apt-key add -
echo "deb https://packages.adoptium.net/artifactory/deb $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/adoptium.list
sudo apt update
sudo apt install -y temurin-21-jdk

# 驗證安裝
java -version
```

### 安裝 Maven

#### Windows (使用 Winget)

```powershell
# 安裝 Maven
winget install Apache.Maven

# 驗證安裝
mvn -version
```

#### Windows (手動安裝)

1. 下載 [Apache Maven](https://maven.apache.org/download.cgi) (Binary zip archive)
2. 解壓縮至 `C:\Program Files\Apache\maven`
3. 設定環境變數：
   - 新增 `MAVEN_HOME` = `C:\Program Files\Apache\maven`
   - 將 `%MAVEN_HOME%\bin` 加入 `PATH`
4. 開啟新的命令提示字元，執行 `mvn -version` 驗證

#### macOS (使用 Homebrew)

```bash
brew install maven
mvn -version
```

#### Linux (Ubuntu/Debian)

```bash
sudo apt update
sudo apt install -y maven
mvn -version
```

### 安裝 Playwright 瀏覽器

專案首次執行時需要安裝 Playwright 瀏覽器：

```bash
# 使用 Maven 執行 Playwright 安裝腳本
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"

# 或安裝所有支援的瀏覽器
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install"
```

### 環境變數設定 (Windows)

如需手動設定環境變數：

```powershell
# 設定 JAVA_HOME (以系統管理員身份執行)
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-21", "Machine")

# 設定 MAVEN_HOME
[System.Environment]::SetEnvironmentVariable("MAVEN_HOME", "C:\Program Files\Apache\maven", "Machine")

# 更新 PATH
$currentPath = [System.Environment]::GetEnvironmentVariable("PATH", "Machine")
$newPath = "$currentPath;%JAVA_HOME%\bin;%MAVEN_HOME%\bin"
[System.Environment]::SetEnvironmentVariable("PATH", $newPath, "Machine")

# 重新開啟命令提示字元後驗證
java -version
mvn -version
```

### 驗證開發環境

```bash
# 檢查 Java 版本 (應為 21.x.x)
java -version

# 檢查 Maven 版本 (應為 3.9+)
mvn -version

# 檢查 Git 版本
git --version

# Clone 並編譯專案以驗證環境
git clone https://github.com/HeimlichLin/etf-holdings-tracker.git
cd etf-holdings-tracker
mvn clean compile -q
echo "環境設定成功！"
```

---

## 🚀 快速開始

### 從原始碼建置

```bash
# Clone 專案
git clone https://github.com/HeimlichLin/etf-holdings-tracker.git
cd etf-holdings-tracker

# 確認 JDK 版本
java -version
# 應顯示: openjdk version "21.x.x"

# 編譯專案（開發時使用）
mvn clean compile -q

# 建置完整套件
mvn clean package -DskipTests
```

### 開發環境啟動

#### 桌面版（JavaFX GUI）

```bash
mvn javafx:run
```

#### 網頁版（REST API）

```bash
mvn spring-boot:run
```

然後在瀏覽器打開：http://localhost:8080

### 建置發行套件

#### 選項 A：標準套件（需要使用者安裝 Java 21）

```bash
mvn clean package -Pdist -DskipTests
```

產生的檔案位於 `target/ETF-Holdings-Tracker-1.0.0-dist.zip`（約 50MB）。

#### 選項 B：內嵌 JRE 套件（免安裝 Java，解壓即可執行）🌟 推薦

```bash
# 步驟 1：下載 JRE
mvn package -Pdist-jre-prepare -DskipTests

# 步驟 2：建置含 JRE 的發行套件
mvn package -Pdist-jre -DskipTests
```

產生的檔案位於 `target/ETF-Holdings-Tracker-1.0.0-windows-x64.zip`（約 92MB）。

> 💡 建議一般使用者使用選項 B，無需額外安裝 Java 即可執行。

### 執行發行套件

1. **從 `target/` 解壓縮發行套件**
   ```bash
   # 在 target/ 目錄中找到 ETF-Holdings-Tracker-1.0.0-windows-x64.zip
   # 右鍵選擇「解壓縮到...」或使用解壓工具
   ```

2. **直接雙擊執行檔啟動應用程式**

| 檔案 | 說明 |
|------|------|
| `ETF-Tracker.vbs` | ✅ 推薦使用 - VBScript 啟動器（雙擊即用，最無痕） |
| `ETF-Tracker.cmd` | 備用選項 - 命令行模式（隱藏控制台視窗） |
| `run.bat` | 除錯選項 - 顯示控制台視窗（用於錯誤診斷） |

> 💡 **提示**：
> - 直接雙擊 `ETF-Tracker.vbs` 即可啟動應用程式
> - 無需安裝 Java，應用程式已自帶 JRE
> - VBScript 提供最佳的用戶體驗，應用程式完全在背景運行
> - 應用程式支援單執行緒鎖定，防止同時開啟多個實例

---

## 📜 Google Apps Script 版本

專案提供獨立的 Google Apps Script (GAS) 版本，可直接在 Google Sheets 中運行，無需安裝任何軟體。

### GAS 版本特色

| 功能 | 說明 |
|------|------|
| 🌐 **雲端執行** | 直接在 Google 雲端運行，無需本地環境 |
| ⏰ **自動排程** | 支援 Google Apps Script 觸發器，定時自動抓取 |
| 📊 **直接寫入 Sheets** | 資料直接儲存至 Google Sheets |
| 🔄 **即時同步** | 多裝置即時存取最新資料 |
| 📝 **日誌記錄** | 執行日誌記錄於獨立工作表 |

### GAS 專案結構

```
gas-scraper/
├── src/
│   ├── appsscript.json      # GAS 專案配置
│   └── EtfScraper.js        # 主要腳本
├── package.json             # Node.js 開發依賴
├── .clasp.json              # clasp 部署配置
└── test-local.js            # 本地測試腳本
```

### 部署 GAS 版本

#### 方法一：手動部署

1. 開啟 [Google Apps Script](https://script.google.com/)
2. 建立新專案
3. 將 `gas-scraper/src/EtfScraper.js` 內容貼入
4. 設定專案屬性 (appsscript.json)
5. 連結至目標 Google Sheets

#### 方法二：使用 clasp 部署

```bash
# 安裝 clasp
npm install -g @google/clasp

# 登入 Google 帳戶
clasp login

# 進入 GAS 專案目錄
cd gas-scraper

# 安裝開發依賴
npm install

# 推送至 Google Apps Script
clasp push

# 開啟線上編輯器
clasp open
```

### GAS 設定觸發器

1. 在 Apps Script 編輯器中，點擊「觸發條件」圖示
2. 點擊「新增觸發條件」
3. 選擇函式：`runDailyJob`
4. 選擇事件來源：「時間驅動」
5. 選擇時間型觸發條件：「每日計時器」
6. 選擇時段：「午夜到凌晨 1 點」

### GAS 主要函式

| 函式 | 說明 |
|------|------|
| `runDailyJob()` | 每日排程主函式，抓取並儲存持倉資料 |
| `fetchHoldingsData()` | 從 ezmoney.com.tw 抓取持倉資料 |
| `saveDailySnapshot()` | 將持倉資料寫入 Google Sheets |
| `cleanupOldData()` | 清理超過 90 天的舊資料 |
| `log()` | 記錄執行日誌至 Logs 工作表 |

### GAS 限制說明

> ⚠️ **注意**：GAS 版本使用 `UrlFetchApp` 抓取資料，無法執行 JavaScript 動態渲染的內容。若目標網站改為 CSR (Client-Side Rendering)，可能需要改用 Java 版本的 Playwright 方案。

---

## 📁 專案結構

```
etf-holdings-tracker/
├── pom.xml                          # Maven 設定檔
├── src/
│   ├── main/
│   │   ├── java/com/etf/tracker/
│   │   │   ├── EtfHoldingsTrackerApplication.java  # Spring Boot 主程式
│   │   │   ├── config/           # 應用程式配置類別
│   │   │   ├── controller/       # REST 端點 (/api/holdings/**)
│   │   │   ├── dto/              # 資料傳輸物件
│   │   │   │   └── mapper/       # DTO 轉換器 (手動映射)
│   │   │   ├── exception/        # 自訂例外 (使用工廠方法)
│   │   │   ├── gui/              # JavaFX 控制器與元件
│   │   │   ├── model/            # 領域模型
│   │   │   ├── scraper/          # 網頁擷取策略
│   │   │   └── service/          # 業務邏輯層
│   │   └── resources/
│   │       ├── application.yml   # 應用程式配置
│   │       ├── logback-spring.xml # JSON 結構化日誌配置
│   │       ├── fxml/             # JavaFX FXML 佈局
│   │       ├── css/              # 樣式表
│   │       └── images/           # 圖示資源
│   └── test/                     # 單元測試與整合測試
├── gas-scraper/                  # Google Apps Script 版本
│   ├── src/
│   │   ├── appsscript.json       # GAS 專案配置
│   │   └── EtfScraper.js         # GAS 主要腳本
│   ├── package.json              # Node.js 開發依賴
│   └── .clasp.json               # clasp 部署配置
├── specs/                        # 規格文件
├── data/                         # Excel 資料儲存目錄 (執行時產生)
└── logs/                         # 日誌目錄
```

### 關鍵目錄說明

| 路徑 | 說明 |
|------|------|
| `controller/` | REST 端點，對應 `/api/holdings/**` |
| `service/` | 業務邏輯層，每個功能一個 Service |
| `scraper/` | 網頁擷取策略，使用 Playwright + Jsoup |
| `dto/` | API 回應物件，搭配 `dto/mapper/` 轉換 |
| `model/` | 領域模型 (`DailySnapshot`, `Holding`, `HoldingChange`) |
| `exception/` | 自訂例外，使用工廠方法建立 |
| `gui/` | JavaFX 控制器與元件 |

---

## 🏗️ 系統架構

```
┌─────────────────────────────────────────────────────────────┐
│ GUI (JavaFX)          │  REST API (Spring MVC)              │
│ MainApp/Controller    │  HoldingController                  │
├───────────────────────┴─────────────────────────────────────┤
│                    Service Layer                            │
│  DataFetchService │ HoldingQueryService │ ExcelStorageService │
│  HoldingCompareService │ DataCleanupService │ ScheduledTaskService │
├─────────────────────────────────────────────────────────────┤
│                    Scraper Layer                            │
│  PlaywrightWebClient → EzMoneyScraperStrategy (Jsoup 解析)  │
│  RetryableWebClient (指數退避重試)                           │
├─────────────────────────────────────────────────────────────┤
│                    Storage Layer                            │
│  ExcelStorageService (本地) │ HybridStorageService (雲端)    │
│  ./data/holdings.xlsx - 透過 Apache POI 讀寫                │
└─────────────────────────────────────────────────────────────┘
```

### 核心資料流

```
1. 資料抓取
   DataFetchService → PlaywrightWebClient.fetchHtml() → EzMoneyScraperStrategy.parseHoldings()

2. 資料模型
   DailySnapshot (日期快照) ─┬─ date: LocalDate
                            ├─ holdings: List<Holding>
                            ├─ totalCount: int
                            └─ totalWeight: BigDecimal

   Holding (個股持倉) ─┬─ stockCode: String (唯一識別碼)
                      ├─ stockName: String
                      ├─ shares: Long
                      └─ weight: BigDecimal

3. 持倉比較
   HoldingCompareService.compareRange() → 計算新進/剔除/增持/減持
```

---

## 🛠️ 技術架構

| 類別 | 技術 | 版本 | 用途 |
|------|------|------|------|
| 語言 | Java | 21 (LTS) | 主要開發語言 |
| 核心框架 | Spring Boot | 3.2.0 | 依賴注入、配置管理、REST API |
| GUI 框架 | JavaFX | 21.0.1 | 桌面介面 |
| HTTP 客戶端 | OkHttp | 4.12.0 | HTTP 請求 |
| 瀏覽器自動化 | Playwright | 1.41.0 | 動態網頁擷取 |
| HTML 解析 | Jsoup | 1.17.2 | 靜態 HTML 解析 |
| Excel 處理 | Apache POI | 5.2.5 | Excel 讀寫 |
| JSON 處理 | Gson | 2.10.1 | JSON 序列化 |
| 日誌記錄 | Logback + Logstash Encoder | 7.4 | JSON 結構化日誌 |
| 測試框架 | JUnit 5 + Mockito | - | 單元/整合測試 |
| GUI 測試 | TestFX | 4.0.18 | JavaFX 測試 |
| 建置工具 | Maven | 3.9+ | 專案建置與依賴管理 |

---

## 🔌 API 端點

### 持倉資料 API

| 方法 | 端點 | 說明 |
|------|------|------|
| `POST` | `/api/holdings/fetch` | 抓取最新持倉資料 |
| `GET` | `/api/holdings/query/{date}` | 單日查詢 (日期格式: `YYYY-MM-DD`) |
| `GET` | `/api/holdings/compare` | 區間比較分析 |
| `DELETE` | `/api/holdings/cleanup` | 清理舊資料 |

### 查詢參數範例

```http
# 區間比較
GET /api/holdings/compare?startDate=2024-01-01&endDate=2024-01-31

# 單日查詢
GET /api/holdings/query/2024-01-15
```

### 回應格式

```json
{
  "success": true,
  "data": {
    "date": "2024-01-15",
    "totalCount": 50,
    "holdings": [
      {
        "stockCode": "2330",
        "stockName": "台積電",
        "shares": 1000000,
        "weight": 15.5
      }
    ]
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

---

## 📖 使用說明

### 抓取資料

1. 啟動應用程式
2. 點擊「更新資料」按鈕
3. 等待資料抓取完成（顯示進度條）

### 查詢持倉

1. 點擊「單日查詢」分頁
2. 選擇日期（預設顯示最新資料）
3. 查看該日期的持倉清單

### 比較持倉

1. 點擊「區間比較」分頁
2. 選擇起始日期和結束日期
3. 查看增減變化
   - 🔴 紅色 = 增持
   - 🟢 綠色 = 減持
   - ➕ 新進成分股
   - ➖ 被剔除成分股

### 清理資料

1. 點擊「資料清理」按鈕
2. 預覽將被清理的資料
3. 確認執行清理（超過 90 天的資料）

### 快捷鍵

| 快捷鍵 | 功能 |
|--------|------|
| `F5` | 更新資料 |
| `Ctrl+Q` | 離開應用程式 |
| `Ctrl+D` | 單日查詢 |
| `Ctrl+R` | 區間比較 |

---

## 🧪 測試

### 執行測試

```bash
# 執行所有測試
mvn test

# 執行測試並產生覆蓋率報告
mvn test jacoco:report

# 查看覆蓋率報告
start target/site/jacoco/index.html
```

### 測試覆蓋率

專案維持 **80% 以上**的程式碼覆蓋率：

| 類型 | 涵蓋範圍 |
|------|----------|
| 單元測試 | 服務層、擷取模組、業務邏輯 |
| 整合測試 | 控制器、API 端點、資料流 |
| GUI 測試 | 視圖控制器（headless 模式） |

### 測試工具

- **JUnit 5** - 測試框架
- **Mockito** - Mock 物件
- **MockMvc** - Spring MVC 測試
- **TestFX** - JavaFX GUI 測試
- **MockWebServer** - HTTP Mock 伺服器
- **JaCoCo** - 覆蓋率報告

---

## 🔧 配置

應用程式配置位於 `src/main/resources/application.yml`：

```yaml
app:
  # 資料儲存配置
  data:
    storage-path: ./data         # Excel 儲存位置
    file-name: holdings.xlsx     # Excel 檔案名稱
    retention-days: 90           # 資料保留天數

  # 網頁擷取配置
  scraper:
    target-url: https://www.ezmoney.com.tw/ETF/Fund/Info?FundCode=49YTW
    timeout-seconds: 10          # 請求逾時時間
    max-retries: 3               # 最大重試次數
    retry-delays: [2, 4, 8]      # 指數退避 (秒)

  # HTTP 客戶端配置
  http-client:
    connect-timeout-seconds: 10
    read-timeout-seconds: 30
    write-timeout-seconds: 30

  # Google Sheets 配置 (可選)
  google-sheets:
    enabled: false               # 是否啟用雲端同步
    spreadsheet-id: YOUR_SHEET_ID
    credentials-path: ./config/google-credentials.json
```

### 日誌配置

日誌檔案位於 `logs/` 目錄，採用 JSON 結構化格式：

| 檔案 | 說明 |
|------|------|
| `app.log` | 主要應用程式日誌 |
| `error.log` | 錯誤日誌 |

---

## 📏 開發規範

### 程式碼品質標準

| 規則 | 要求 |
|------|------|
| 方法長度 | ≤ 50 行 |
| 類別長度 | ≤ 500 行 |
| 循環複雜度 | ≤ 10 |
| 測試覆蓋率 | ≥ 80% |
| 公開 API | 必須有 Javadoc |
| 日期格式 | ISO 8601 (`YYYY-MM-DD`) |

### 命名規範

| 類型 | 格式 | 範例 |
|------|------|------|
| 整合測試 | `*IT.java` | `HoldingControllerCompareIT.java` |
| 單元測試 | `*Test.java` | `DataFetchServiceTest.java` |
| DTO 轉換器 | `*Mapper.java` | `HoldingMapper.java` |
| 例外類別 | `*Exception.java` | `DataFetchException.java` |

### 例外處理模式

使用工廠方法建立例外，保留完整上下文：

```java
// ✅ 正確：使用工廠方法
throw DataFetchException.timeout(url);
throw DataFetchException.httpError(url, 500);
throw DataFetchException.parseError(content, cause);

// ❌ 避免：直接建構
throw new DataFetchException("連線失敗");
```

### 測試規範

- 測試使用 `@ActiveProfiles("test")` 啟用 `application-test.yml`
- 測試資料寫入 `./target/test-data/`
- 使用 `@Nested` 分組相關測試案例
- MockMvc 測試 REST API
- Mockito 測試 Service 層

---

## 🤝 貢獻指南

歡迎提交 Issue 和 Pull Request！

### 開發流程

1. Fork 此專案
2. 建立功能分支
   ```bash
   git checkout -b feature/AmazingFeature
   ```
3. 提交變更
   ```bash
   git commit -m 'Add some AmazingFeature'
   ```
4. 推送到分支
   ```bash
   git push origin feature/AmazingFeature
   ```
5. 開啟 Pull Request

### 開發協作

- 使用 `.github/prompts/` 中的 slash command 範本進行開發協作
- 遵循 `.github/copilot-instructions.md` 中的專案慣例
- 確保所有測試通過
- 維持 80% 以上測試覆蓋率
- 公開 API 必須有 Javadoc 註解

### Code Review 檢查清單

- [ ] 程式碼符合命名規範
- [ ] 方法長度 ≤ 50 行
- [ ] 新增適當的單元/整合測試
- [ ] 更新相關文件
- [ ] 例外處理使用工廠方法

---

## 📚 文件資源

詳細規格文件位於 `specs/001-etf-holdings-tracker/`:

| 檔案 | 說明 |
|------|------|
| [`spec.md`](specs/001-etf-holdings-tracker/spec.md) | 功能規格與 User Stories |
| [`plan.md`](specs/001-etf-holdings-tracker/plan.md) | 實作計畫與技術決策 |
| [`data-model.md`](specs/001-etf-holdings-tracker/data-model.md) | 資料模型定義 |
| [`tasks.md`](specs/001-etf-holdings-tracker/tasks.md) | 任務清單與進度追蹤 |
| [`quickstart.md`](specs/001-etf-holdings-tracker/quickstart.md) | 快速入門指南 |
| [`research.md`](specs/001-etf-holdings-tracker/research.md) | 技術研究與選型決策 |

---

## 📄 授權

本專案採用 MIT 授權條款。詳見 [LICENSE](LICENSE) 檔案。

---

## 👥 作者

- **HeimlichLin** - [@HeimlichLin](https://github.com/HeimlichLin)

---

<div align="center">

**ETF Holdings Tracker** - 讓 ETF 投資更透明 📈

[回報問題](https://github.com/HeimlichLin/etf-holdings-tracker/issues) · [功能建議](https://github.com/HeimlichLin/etf-holdings-tracker/issues) · [貢獻程式碼](https://github.com/HeimlichLin/etf-holdings-tracker/pulls)

</div>
