# 內部服務層 API 合約

**Branch**: `001-etf-holdings-tracker` | **Date**: 2025-11-26  
**Status**: Phase 1 設計

---

## 概述

此文件定義應用程式內部服務層的 API 合約。由於這是桌面應用程式，使用 `@RestController` 提供內部 REST API 供 JavaFX GUI 層調用。

> **注意**: 使用標準 `@RestController` 類別，不使用 RouterFunctions。

---

## 1. 通用回應格式

### 1.1 成功回應

```json
{
  "success": true,
  "data": { ... },
  "meta": {
    "timestamp": "2025-11-26T10:30:00Z",
    "correlationId": "abc-123-def-456"
  },
  "errors": null
}
```

### 1.2 錯誤回應

```json
{
  "success": false,
  "data": null,
  "meta": {
    "timestamp": "2025-11-26T10:30:00Z",
    "correlationId": "abc-123-def-456"
  },
  "errors": [
    {
      "code": "DATA_FETCH_FAILED",
      "message": "無法取得資料，請檢查網路連線",
      "field": null
    }
  ]
}
```

### 1.3 Java 定義

```java
package com.etf.tracker.dto;

import java.time.Instant;
import java.util.List;

public record ApiResponse<T>(
    boolean success,
    T data,
    Meta meta,
    List<ApiError> errors
) {
    public record Meta(Instant timestamp, String correlationId) {}
    public record ApiError(String code, String message, String field) {}
    
    public static <T> ApiResponse<T> success(T data, String correlationId) {
        return new ApiResponse<>(true, data, new Meta(Instant.now(), correlationId), null);
    }
    
    public static <T> ApiResponse<T> error(String code, String message, String correlationId) {
        return new ApiResponse<>(false, null, 
            new Meta(Instant.now(), correlationId), 
            List.of(new ApiError(code, message, null)));
    }
}
```

---

## 2. 持倉資料 API

### 2.1 抓取最新持倉資料

**Endpoint**: `POST /api/holdings/fetch`

**Description**: 從外部資料來源抓取 ETF 00981A 的最新持倉資料並儲存

**Request Body**: 無

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "date": "2025-11-26",
    "holdings": [
      {
        "stockCode": "2330",
        "stockName": "台積電",
        "shares": 1234567,
        "weight": 12.3456
      }
    ],
    "totalCount": 50,
    "totalWeight": 99.9876
  },
  "meta": { ... },
  "errors": null
}
```

**Error Codes**:
| Code | Message | HTTP Status |
|------|---------|-------------|
| DATA_FETCH_FAILED | 無法取得資料，請檢查網路連線 | 503 |
| DATA_PARSE_FAILED | 資料解析失敗，網站結構可能已變更 | 500 |
| DATA_ALREADY_EXISTS | 今日資料已存在，是否覆蓋？ | 409 |
| STORAGE_ERROR | 儲存失敗，檔案可能被其他程式佔用 | 500 |

**Controller 定義**:
```java
@RestController
@RequestMapping("/api/holdings")
public class HoldingController {
    
    @PostMapping("/fetch")
    public ResponseEntity<ApiResponse<DailySnapshotDto>> fetchLatestHoldings(
            @RequestParam(defaultValue = "false") boolean overwrite) {
        // Implementation
    }
}
```

---

### 2.2 查詢單日持倉資料

**Endpoint**: `GET /api/holdings/{date}`

**Description**: 取得指定日期的持倉資料

**Path Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| date | String | Yes | 日期格式 yyyy-MM-dd |

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "date": "2025-11-26",
    "holdings": [
      {
        "stockCode": "2330",
        "stockName": "台積電",
        "shares": 1234567,
        "weight": 12.3456
      }
    ],
    "totalCount": 50,
    "totalWeight": 99.9876
  },
  "meta": { ... },
  "errors": null
}
```

**Error Codes**:
| Code | Message | HTTP Status |
|------|---------|-------------|
| DATA_NOT_FOUND | 該日期無資料 | 404 |
| INVALID_DATE_FORMAT | 日期格式錯誤 | 400 |

**Controller 定義**:
```java
@GetMapping("/{date}")
public ResponseEntity<ApiResponse<DailySnapshotDto>> getHoldingsByDate(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    // Implementation
}
```

---

### 2.3 查詢最新持倉資料

**Endpoint**: `GET /api/holdings/latest`

**Description**: 取得系統中最新的持倉資料

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "date": "2025-11-26",
    "holdings": [ ... ],
    "totalCount": 50,
    "totalWeight": 99.9876
  },
  "meta": { ... },
  "errors": null
}
```

**Error Codes**:
| Code | Message | HTTP Status |
|------|---------|-------------|
| NO_DATA_AVAILABLE | 系統尚無任何持倉資料 | 404 |

**Controller 定義**:
```java
@GetMapping("/latest")
public ResponseEntity<ApiResponse<DailySnapshotDto>> getLatestHoldings() {
    // Implementation
}
```

---

### 2.4 取得可查詢日期清單

**Endpoint**: `GET /api/holdings/available-dates`

**Description**: 取得系統中所有可查詢的日期清單

**Query Parameters**:
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| limit | Integer | No | 90 | 最多回傳筆數 |

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "dates": [
      "2025-11-26",
      "2025-11-25",
      "2025-11-24"
    ],
    "totalCount": 45
  },
  "meta": { ... },
  "errors": null
}
```

**Controller 定義**:
```java
@GetMapping("/available-dates")
public ResponseEntity<ApiResponse<AvailableDatesDto>> getAvailableDates(
        @RequestParam(defaultValue = "90") int limit) {
    // Implementation
}
```

---

## 3. 區間比較 API

### 3.1 比較兩日期間的持倉變化

**Endpoint**: `GET /api/holdings/compare`

**Description**: 計算兩個日期間的持倉增減變化

**Query Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| startDate | String | Yes | 起始日期 yyyy-MM-dd |
| endDate | String | Yes | 結束日期 yyyy-MM-dd |

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "startDate": "2025-11-20",
    "endDate": "2025-11-26",
    "newAdditions": [
      {
        "stockCode": "2454",
        "stockName": "聯發科",
        "changeType": "NEW_ADDITION",
        "startShares": null,
        "endShares": 500000,
        "sharesDiff": 500000,
        "changeRatio": null,
        "startWeight": null,
        "endWeight": 5.1234,
        "weightDiff": 5.1234
      }
    ],
    "removals": [
      {
        "stockCode": "2412",
        "stockName": "中華電",
        "changeType": "REMOVED",
        "startShares": 300000,
        "endShares": null,
        "sharesDiff": -300000,
        "changeRatio": -100.0,
        "startWeight": 3.5000,
        "endWeight": null,
        "weightDiff": -3.5000
      }
    ],
    "increased": [
      {
        "stockCode": "2330",
        "stockName": "台積電",
        "changeType": "INCREASED",
        "startShares": 1200000,
        "endShares": 1234567,
        "sharesDiff": 34567,
        "changeRatio": 2.88,
        "startWeight": 12.0000,
        "endWeight": 12.3456,
        "weightDiff": 0.3456
      }
    ],
    "decreased": [
      {
        "stockCode": "2317",
        "stockName": "鴻海",
        "changeType": "DECREASED",
        "startShares": 1000000,
        "endShares": 987654,
        "sharesDiff": -12346,
        "changeRatio": -1.23,
        "startWeight": 9.0000,
        "endWeight": 8.7654,
        "weightDiff": -0.2346
      }
    ],
    "unchanged": [ ... ],
    "summary": {
      "totalNewAdditions": 1,
      "totalRemovals": 1,
      "totalIncreased": 5,
      "totalDecreased": 8,
      "totalUnchanged": 35
    }
  },
  "meta": { ... },
  "errors": null
}
```

**Error Codes**:
| Code | Message | HTTP Status |
|------|---------|-------------|
| START_DATE_NOT_FOUND | 起始日期無資料 | 404 |
| END_DATE_NOT_FOUND | 結束日期無資料 | 404 |
| INVALID_DATE_RANGE | 起始日期必須早於結束日期 | 400 |
| INVALID_DATE_FORMAT | 日期格式錯誤 | 400 |

**Controller 定義**:
```java
@GetMapping("/compare")
public ResponseEntity<ApiResponse<RangeCompareResultDto>> compareHoldings(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    // Implementation
}
```

---

## 4. 資料清理 API

### 4.1 清理過期資料

**Endpoint**: `DELETE /api/holdings/cleanup`

**Description**: 手動清理超過 90 天的歷史資料

**Query Parameters**:
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| daysToKeep | Integer | No | 90 | 保留天數 |
| confirm | Boolean | Yes | - | 確認刪除 (必須為 true) |

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "deletedDates": [
      "2025-08-20",
      "2025-08-19",
      "2025-08-18"
    ],
    "deletedCount": 3,
    "remainingCount": 87
  },
  "meta": { ... },
  "errors": null
}
```

**Error Codes**:
| Code | Message | HTTP Status |
|------|---------|-------------|
| CONFIRMATION_REQUIRED | 請確認刪除操作 | 400 |
| CLEANUP_FAILED | 清理作業失敗 | 500 |

**Controller 定義**:
```java
@DeleteMapping("/cleanup")
public ResponseEntity<ApiResponse<CleanupResultDto>> cleanupOldData(
        @RequestParam(defaultValue = "90") int daysToKeep,
        @RequestParam boolean confirm) {
    // Implementation
}
```

---

## 5. 系統狀態 API

### 5.1 健康檢查

**Endpoint**: `GET /api/system/health`

**Description**: 檢查系統運作狀態

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "status": "UP",
    "dataFileExists": true,
    "dataFilePath": "./data/holdings.xlsx",
    "dataFileSize": 102400,
    "latestDataDate": "2025-11-26",
    "totalRecords": 4500
  },
  "meta": { ... },
  "errors": null
}
```

**Controller 定義**:
```java
@RestController
@RequestMapping("/api/system")
public class SystemController {
    
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<SystemHealthDto>> getHealth() {
        // Implementation
    }
}
```

---

## 6. 錯誤碼對照表

| Code | HTTP Status | 使用者訊息 |
|------|-------------|-----------|
| DATA_FETCH_FAILED | 503 | 無法取得資料，請檢查網路連線後再試 |
| DATA_PARSE_FAILED | 500 | 資料解析失敗，網站結構可能已變更 |
| DATA_ALREADY_EXISTS | 409 | 今日資料已存在，是否覆蓋？ |
| DATA_NOT_FOUND | 404 | 該日期無資料 |
| NO_DATA_AVAILABLE | 404 | 系統尚無任何持倉資料 |
| STORAGE_ERROR | 500 | 儲存失敗，檔案可能被其他程式佔用 |
| INVALID_DATE_FORMAT | 400 | 日期格式錯誤，請使用 yyyy-MM-dd 格式 |
| INVALID_DATE_RANGE | 400 | 起始日期必須早於結束日期 |
| START_DATE_NOT_FOUND | 404 | 起始日期無資料 |
| END_DATE_NOT_FOUND | 404 | 結束日期無資料 |
| CONFIRMATION_REQUIRED | 400 | 請確認刪除操作 |
| CLEANUP_FAILED | 500 | 清理作業失敗 |
| INTERNAL_ERROR | 500 | 系統發生錯誤，請稍後再試 |

---

## 7. Controller 類別結構

```java
package com.etf.tracker.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/holdings")
public class HoldingController {
    
    private final DataFetchService dataFetchService;
    private final HoldingQueryService holdingQueryService;
    private final HoldingCompareService holdingCompareService;
    private final ExcelStorageService excelStorageService;
    
    // Constructor injection
    
    @PostMapping("/fetch")
    public ResponseEntity<ApiResponse<DailySnapshotDto>> fetchLatestHoldings(...) { }
    
    @GetMapping("/{date}")
    public ResponseEntity<ApiResponse<DailySnapshotDto>> getHoldingsByDate(...) { }
    
    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<DailySnapshotDto>> getLatestHoldings() { }
    
    @GetMapping("/available-dates")
    public ResponseEntity<ApiResponse<AvailableDatesDto>> getAvailableDates(...) { }
    
    @GetMapping("/compare")
    public ResponseEntity<ApiResponse<RangeCompareResultDto>> compareHoldings(...) { }
    
    @DeleteMapping("/cleanup")
    public ResponseEntity<ApiResponse<CleanupResultDto>> cleanupOldData(...) { }
}
```

```java
package com.etf.tracker.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system")
public class SystemController {
    
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<SystemHealthDto>> getHealth() { }
}
```
