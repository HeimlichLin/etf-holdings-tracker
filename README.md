# ETF Holdings Tracker
追蹤特定主動型 ETF 的成分股每日變化

## 功能特色

- **每日成分股追蹤**: 自動爬取並記錄 ETF 成分股資料
- **變動比較**: 與前一天資料比較，記錄新增、移除、增持、減持等變化
- **歷史記錄**: 保留最近一季 (90天) 的變動資料，用於趨勢分析
- **REST API**: 提供完整的 API 介面查詢成分股和變動記錄
- **排程執行**: 自動在交易日收盤後執行追蹤任務

## 技術架構

- **Java 17**
- **Spring Boot 3.2**
- **Spring Data JPA**
- **H2 Database** (嵌入式資料庫)
- **Maven** (建置工具)
- **Jsoup** (網頁爬蟲)

## 支援的 ETF

目前支援 ARK Invest 系列 ETF:
- ARKK - ARK Innovation ETF
- ARKW - ARK Next Generation Internet ETF
- ARKQ - ARK Autonomous Technology & Robotics ETF
- ARKG - ARK Genomic Revolution ETF
- ARKF - ARK Fintech Innovation ETF
- ARKX - ARK Space Exploration & Innovation ETF

## 快速開始

### 建置專案

```bash
mvn clean package
```

### 執行應用程式

```bash
java -jar target/etf-holdings-tracker-1.0.0-SNAPSHOT.jar
```

或使用 Maven:

```bash
mvn spring-boot:run
```

### 存取 H2 Console (開發用)

啟動後可透過 http://localhost:8080/h2-console 存取資料庫

## API 文件

### 追蹤 ETF 成分股變動
```
POST /api/etf/{symbol}/track
```

### 查詢當前成分股
```
GET /api/etf/{symbol}/holdings
```

### 查詢指定日期範圍變動
```
GET /api/etf/{symbol}/changes?startDate=2024-01-01&endDate=2024-03-31
```

### 查詢最近一季變動
```
GET /api/etf/{symbol}/changes/quarter
```

### 查詢特定股票變動歷史
```
GET /api/etf/{symbol}/stock/{stockSymbol}/history
```

### 手動輸入成分股
```
POST /api/etf/{symbol}/holdings
Content-Type: application/json

[
  {
    "stockSymbol": "TSLA",
    "stockName": "Tesla Inc",
    "shares": 1000000,
    "marketValue": 500000000,
    "weight": 10.5
  }
]
```

### 清理過期資料
```
POST /api/etf/cleanup
```

## 配置說明

在 `application.yml` 中可調整以下配置:

```yaml
etf:
  # 追蹤的 ETF 代碼
  tracked-symbols: ARKK,ARKW,ARKQ,ARKG,ARKF
  
  # 資料保留天數
  data:
    retention-days: 90
  
  # 排程 (cron 格式)
  scheduler:
    cron: "0 0 22 * * MON-FRI"
    cleanup-cron: "0 0 0 * * SUN"
```

## 變動類型說明

| 類型 | 說明 |
|------|------|
| NEW | 新增成分股 |
| REMOVED | 移除成分股 |
| INCREASED | 增持 (持股數量增加) |
| DECREASED | 減持 (持股數量減少) |

## 授權

MIT License
