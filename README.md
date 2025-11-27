# ETF Holdings Tracker

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-blue.svg)](https://openjfx.io/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

追蹤 ETF 00981A 每日持倉變化的 Windows 桌面應用程式。

## 🎯 功能特色

- **📥 自動抓取資料** - 從網站自動抓取最新 ETF 成分股資料
- **📊 單日查詢** - 查看特定日期的持倉資料
- **📈 區間比較** - 比較兩個日期間的持倉變化（紅增綠減）
- **🔄 變化分類** - 自動分類新進/剔除/增持/減持股票
- **🗂️ Excel 儲存** - 資料本地儲存為 Excel 格式
- **🧹 資料清理** - 自動清理超過 90 天的舊資料

## 📋 系統需求

### 開發環境

| 項目 | 版本 |
|------|------|
| JDK | 21 (LTS) |
| Maven | 3.9+ |
| IDE | IntelliJ IDEA / VS Code |

### 執行環境

| 項目 | 需求 |
|------|------|
| 作業系統 | Windows 10/11 (x64) |
| 網路連線 | 需要 (用於抓取資料) |
| Java | 不需要 (應用程式自帶 JRE) |

## 🚀 快速開始

### 從原始碼建置

```bash
# Clone 專案
git clone https://github.com/HeimlichLin/etf-holdings-tracker.git
cd etf-holdings-tracker

# 編譯專案（開發時使用）
mvn clean compile -q

# 建置完整套件
mvn clean package -DskipTests
```

### 開發環境啟動應用程式

#### 啟動桌面版（JavaFX GUI）

```bash
mvn javafx:run
```

#### 啟動網頁版（REST API）

```bash
mvn spring-boot:run
```

然後在瀏覽器打開：http://localhost:8080

### 建置發行套件

有兩種發行套件選項：

#### 選項 A：標準套件（需要使用者安裝 Java 21）

```bash
# 建置標準發行套件
mvn clean package -Pdist -DskipTests
```

產生的檔案位於 `target/ETF-Holdings-Tracker-1.0.0-dist.zip`（約 50MB）。

#### 選項 B：內嵌 JRE 套件（免安裝 Java，解壓即可執行）

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

## 📁 專案結構

```
etf-holdings-tracker/
├── src/
│   ├── main/
│   │   ├── java/com/etf/tracker/
│   │   │   ├── config/          # 配置類別
│   │   │   ├── controller/      # REST 控制器
│   │   │   ├── dto/             # 資料傳輸物件
│   │   │   ├── exception/       # 例外處理
│   │   │   ├── gui/             # JavaFX GUI
│   │   │   ├── model/           # 領域模型
│   │   │   ├── scraper/         # 網頁擷取
│   │   │   └── service/         # 業務邏輯
│   │   └── resources/
│   │       ├── fxml/            # JavaFX FXML 佈局
│   │       ├── css/             # 樣式表
│   │       └── images/          # 圖示資源
│   └── test/                    # 單元測試與整合測試
├── specs/                       # 規格文件
├── data/                        # 資料儲存目錄 (執行時產生)
└── logs/                        # 日誌目錄
```

## 🛠️ 技術架構

| 層級 | 技術 |
|------|------|
| 核心框架 | Spring Boot 3.2 |
| GUI 框架 | JavaFX 21 |
| HTTP 客戶端 | OkHttp 4.x |
| HTML 解析 | Jsoup 1.17 |
| Excel 處理 | Apache POI 5.x |
| 日誌記錄 | Logback + JSON 結構化日誌 |
| 測試框架 | JUnit 5 + Mockito + TestFX |

## 📖 使用說明

### 抓取資料

1. 啟動應用程式
2. 點擊「更新資料」按鈕
3. 等待資料抓取完成

### 查詢持倉

1. 點擊「單日查詢」分頁
2. 選擇日期
3. 查看該日期的持倉清單

### 比較持倉

1. 點擊「區間比較」分頁
2. 選擇起始日期和結束日期
3. 查看增減變化（紅色=增持，綠色=減持）

### 清理資料

1. 點擊「資料清理」按鈕
2. 確認要刪除的日期範圍
3. 點擊確認執行清理

## ⌨️ 快捷鍵

| 快捷鍵 | 功能 |
|--------|------|
| `F5` | 更新資料 |
| `Ctrl+Q` | 離開應用程式 |
| `Ctrl+D` | 單日查詢 |
| `Ctrl+R` | 區間比較 |

## 🧪 執行測試

```bash
# 執行所有測試
mvn test

# 執行測試並產生覆蓋率報告
mvn test jacoco:report

# 查看覆蓋率報告
start target/site/jacoco/index.html
```

## 📊 測試覆蓋率

專案維持 80% 以上的程式碼覆蓋率，包含：

- 單元測試：服務層、擷取模組
- 整合測試：控制器、API 端點
- GUI 測試：視圖控制器（headless 模式）

## 🔧 配置

應用程式配置位於 `src/main/resources/application.yml`：

```yaml
etf:
  scraper:
    url: https://www.ezmoney.com.tw/...
    timeout: 10s
    retry-count: 3
  storage:
    path: ./data
    filename: holdings.xlsx
  data:
    retention-days: 90
```

## 📝 日誌

日誌檔案位於 `logs/` 目錄，採用 JSON 結構化格式：

- `app.log` - 主要應用程式日誌
- `error.log` - 錯誤日誌

## 🤝 貢獻

歡迎提交 Issue 和 Pull Request！

## 📄 授權

本專案採用 MIT 授權條款。詳見 [LICENSE](LICENSE) 檔案。

## 👥 作者

- ETF Tracker Team

---

**ETF Holdings Tracker** - 讓 ETF 投資更透明 📈
