# 資料模型: ETF 00981A 每日持倉追蹤系統

**Branch**: `001-etf-holdings-tracker` | **Date**: 2025-11-26  
**Status**: Phase 1 設計

---

## 1. 核心實體 (Domain Entities)

### 1.1 Holding (成分股)

代表 ETF 中的單一持股。

```java
package com.etf.tracker.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 成分股實體
 * 代表 ETF 中的單一持股資訊
 */
public class Holding {
    
    /** 股票代號 (唯一識別碼) */
    private String stockCode;
    
    /** 股票名稱 */
    private String stockName;
    
    /** 持股股數 */
    private Long shares;
    
    /** 持股權重 (百分比) */
    private BigDecimal weight;
    
    // Constructors, Getters, Setters, equals, hashCode, toString
}
```

**欄位規格**:

| Field | Type | Nullable | Validation | Description |
|-------|------|----------|------------|-------------|
| stockCode | String | No | 非空, 最大 10 字元 | 股票代號，如 "2330" |
| stockName | String | No | 非空, 最大 100 字元 | 股票名稱，如 "台積電" |
| shares | Long | No | >= 0 | 持股股數 |
| weight | BigDecimal | No | >= 0, <= 100, 精度 4 位 | 持股權重百分比 |

**業務規則**:
- `stockCode` 作為唯一識別碼，即使股票名稱變更仍視為同一股票
- 權重精度為小數點後 4 位 (如 12.3456%)

---

### 1.2 DailySnapshot (每日快照)

代表特定日期的完整持倉記錄。

```java
package com.etf.tracker.model;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日快照實體
 * 代表特定日期的完整持倉記錄
 */
public class DailySnapshot {
    
    /** 資料日期 */
    private LocalDate date;
    
    /** 該日所有成分股資料 */
    private List<Holding> holdings;
    
    /** 成分股總數 */
    private int totalCount;
    
    /** 總權重 (應接近 100%) */
    private BigDecimal totalWeight;
    
    // Constructors, Getters, Setters
}
```

**欄位規格**:

| Field | Type | Nullable | Validation | Description |
|-------|------|----------|------------|-------------|
| date | LocalDate | No | 有效日期 | 資料日期 |
| holdings | List\<Holding\> | No | 非空清單 | 成分股清單 |
| totalCount | int | No | >= 0 | 成分股總數 |
| totalWeight | BigDecimal | No | 約等於 100 | 總權重百分比 |

---

### 1.3 HoldingChange (持倉變化)

代表兩個日期間單一成分股的變化。

```java
package com.etf.tracker.model;

import java.math.BigDecimal;

/**
 * 持倉變化實體
 * 代表兩個日期間單一成分股的增減變化
 */
public class HoldingChange {
    
    /** 股票代號 */
    private String stockCode;
    
    /** 股票名稱 */
    private String stockName;
    
    /** 變化類型 */
    private ChangeType changeType;
    
    /** 起始日股數 */
    private Long startShares;
    
    /** 結束日股數 */
    private Long endShares;
    
    /** 增減股數 */
    private Long sharesDiff;
    
    /** 變化比例 (百分比) */
    private BigDecimal changeRatio;
    
    /** 起始日權重 */
    private BigDecimal startWeight;
    
    /** 結束日權重 */
    private BigDecimal endWeight;
    
    /** 權重變化 */
    private BigDecimal weightDiff;
    
    // Constructors, Getters, Setters
}
```

**欄位規格**:

| Field | Type | Nullable | Validation | Description |
|-------|------|----------|------------|-------------|
| stockCode | String | No | 非空 | 股票代號 |
| stockName | String | No | 非空 | 股票名稱 |
| changeType | ChangeType | No | Enum | 變化類型 |
| startShares | Long | Yes | >= 0 | 起始日股數 (新進為 null) |
| endShares | Long | Yes | >= 0 | 結束日股數 (剔除為 null) |
| sharesDiff | Long | No | 可正可負 | 增減股數 |
| changeRatio | BigDecimal | Yes | 百分比 | 變化比例 |
| startWeight | BigDecimal | Yes | >= 0 | 起始日權重 |
| endWeight | BigDecimal | Yes | >= 0 | 結束日權重 |
| weightDiff | BigDecimal | No | 可正可負 | 權重變化 |

---

## 2. 列舉類型 (Enums)

### 2.1 ChangeType (變化類型)

```java
package com.etf.tracker.model;

/**
 * 持倉變化類型列舉
 */
public enum ChangeType {
    
    /** 新進增持 - 起始日不存在，結束日新增 */
    NEW_ADDITION("新進"),
    
    /** 剔除減持 - 起始日存在，結束日移除 */
    REMOVED("剔除"),
    
    /** 增持 - 股數增加 */
    INCREASED("增持"),
    
    /** 減持 - 股數減少 */
    DECREASED("減持"),
    
    /** 不變 - 股數相同 */
    UNCHANGED("不變");
    
    private final String displayName;
    
    ChangeType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
```

---

## 3. 資料傳輸物件 (DTOs)

### 3.1 HoldingDto

```java
package com.etf.tracker.dto;

import java.math.BigDecimal;

/**
 * 成分股 DTO
 * 使用 Java Record 實現不可變資料載體
 */
public record HoldingDto(
    String stockCode,
    String stockName,
    Long shares,
    BigDecimal weight
) {}
```

### 3.2 DailySnapshotDto

```java
package com.etf.tracker.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日快照 DTO
 */
public record DailySnapshotDto(
    LocalDate date,
    List<HoldingDto> holdings,
    int totalCount,
    BigDecimal totalWeight
) {}
```

### 3.3 HoldingChangeDto

```java
package com.etf.tracker.dto;

import java.math.BigDecimal;
import com.etf.tracker.model.ChangeType;

/**
 * 持倉變化 DTO
 */
public record HoldingChangeDto(
    String stockCode,
    String stockName,
    ChangeType changeType,
    Long startShares,
    Long endShares,
    Long sharesDiff,
    BigDecimal changeRatio,
    BigDecimal startWeight,
    BigDecimal endWeight,
    BigDecimal weightDiff
) {}
```

### 3.4 RangeCompareResultDto

```java
package com.etf.tracker.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 區間比較結果 DTO
 */
public record RangeCompareResultDto(
    LocalDate startDate,
    LocalDate endDate,
    List<HoldingChangeDto> newAdditions,    // 新進增持
    List<HoldingChangeDto> removals,         // 剔除減持
    List<HoldingChangeDto> increased,        // 增持
    List<HoldingChangeDto> decreased,        // 減持
    List<HoldingChangeDto> unchanged         // 不變
) {}
```

---

## 4. 手動映射器 (Manual Mappers)

### 4.1 HoldingMapper

```java
package com.etf.tracker.dto.mapper;

import com.etf.tracker.dto.HoldingDto;
import com.etf.tracker.model.Holding;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 成分股手動映射器
 * 提供 Entity ↔ DTO 轉換方法
 */
public final class HoldingMapper {
    
    private HoldingMapper() {
        // 禁止實例化
    }
    
    /**
     * Entity → DTO
     */
    public static HoldingDto toDto(Holding entity) {
        if (entity == null) {
            return null;
        }
        return new HoldingDto(
            entity.getStockCode(),
            entity.getStockName(),
            entity.getShares(),
            entity.getWeight()
        );
    }
    
    /**
     * DTO → Entity
     */
    public static Holding toEntity(HoldingDto dto) {
        if (dto == null) {
            return null;
        }
        Holding entity = new Holding();
        entity.setStockCode(dto.stockCode());
        entity.setStockName(dto.stockName());
        entity.setShares(dto.shares());
        entity.setWeight(dto.weight());
        return entity;
    }
    
    /**
     * Entity List → DTO List
     */
    public static List<HoldingDto> toDtoList(List<Holding> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
            .map(HoldingMapper::toDto)
            .collect(Collectors.toList());
    }
    
    /**
     * DTO List → Entity List
     */
    public static List<Holding> toEntityList(List<HoldingDto> dtos) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream()
            .map(HoldingMapper::toEntity)
            .collect(Collectors.toList());
    }
}
```

### 4.2 DailySnapshotMapper

```java
package com.etf.tracker.dto.mapper;

import com.etf.tracker.dto.DailySnapshotDto;
import com.etf.tracker.model.DailySnapshot;

/**
 * 每日快照手動映射器
 */
public final class DailySnapshotMapper {
    
    private DailySnapshotMapper() {}
    
    public static DailySnapshotDto toDto(DailySnapshot entity) {
        if (entity == null) {
            return null;
        }
        return new DailySnapshotDto(
            entity.getDate(),
            HoldingMapper.toDtoList(entity.getHoldings()),
            entity.getTotalCount(),
            entity.getTotalWeight()
        );
    }
    
    public static DailySnapshot toEntity(DailySnapshotDto dto) {
        if (dto == null) {
            return null;
        }
        DailySnapshot entity = new DailySnapshot();
        entity.setDate(dto.date());
        entity.setHoldings(HoldingMapper.toEntityList(dto.holdings()));
        entity.setTotalCount(dto.totalCount());
        entity.setTotalWeight(dto.totalWeight());
        return entity;
    }
}
```

### 4.3 HoldingChangeMapper

```java
package com.etf.tracker.dto.mapper;

import com.etf.tracker.dto.HoldingChangeDto;
import com.etf.tracker.model.HoldingChange;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 持倉變化手動映射器
 */
public final class HoldingChangeMapper {
    
    private HoldingChangeMapper() {}
    
    public static HoldingChangeDto toDto(HoldingChange entity) {
        if (entity == null) {
            return null;
        }
        return new HoldingChangeDto(
            entity.getStockCode(),
            entity.getStockName(),
            entity.getChangeType(),
            entity.getStartShares(),
            entity.getEndShares(),
            entity.getSharesDiff(),
            entity.getChangeRatio(),
            entity.getStartWeight(),
            entity.getEndWeight(),
            entity.getWeightDiff()
        );
    }
    
    public static List<HoldingChangeDto> toDtoList(List<HoldingChange> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
            .map(HoldingChangeMapper::toDto)
            .collect(Collectors.toList());
    }
}
```

---

## 5. 驗證規則 (Validation Rules)

### 5.1 Holding 驗證

```java
public class HoldingValidator {
    
    public static void validate(Holding holding) {
        if (holding == null) {
            throw new ValidationException("成分股資料不可為空");
        }
        if (holding.getStockCode() == null || holding.getStockCode().isBlank()) {
            throw new ValidationException("股票代號不可為空");
        }
        if (holding.getStockCode().length() > 10) {
            throw new ValidationException("股票代號長度不可超過 10 字元");
        }
        if (holding.getStockName() == null || holding.getStockName().isBlank()) {
            throw new ValidationException("股票名稱不可為空");
        }
        if (holding.getShares() == null || holding.getShares() < 0) {
            throw new ValidationException("持股股數必須為非負數");
        }
        if (holding.getWeight() == null) {
            throw new ValidationException("持股權重不可為空");
        }
        if (holding.getWeight().compareTo(BigDecimal.ZERO) < 0 ||
            holding.getWeight().compareTo(new BigDecimal("100")) > 0) {
            throw new ValidationException("持股權重必須介於 0 至 100 之間");
        }
    }
}
```

---

## 6. 狀態轉換 (State Transitions)

### 6.1 HoldingChange 計算邏輯

```
+------------------+     +------------------+
|  起始日持倉       |     |  結束日持倉       |
|  (startSnapshot) | --> |  (endSnapshot)   |
+------------------+     +------------------+
         |                       |
         v                       v
+------------------------------------------------+
|              比較邏輯 (Compare Logic)           |
+------------------------------------------------+
         |
         v
+------------------------------------------------+
|  分類結果:                                      |
|  - NEW_ADDITION: 起始日無，結束日有              |
|  - REMOVED: 起始日有，結束日無                   |
|  - INCREASED: 結束日股數 > 起始日股數            |
|  - DECREASED: 結束日股數 < 起始日股數            |
|  - UNCHANGED: 股數相同                          |
+------------------------------------------------+
```

### 6.2 變化計算公式

```
sharesDiff = endShares - startShares

changeRatio = (sharesDiff / startShares) * 100   // 若 startShares = 0，則為 null

weightDiff = endWeight - startWeight
```

---

## 7. Excel 儲存結構

### 7.1 Holdings Sheet 欄位定義

| Column | Header | Type | Format | Example |
|--------|--------|------|--------|---------|
| A | 日期 | Date | yyyy-MM-dd | 2025-11-26 |
| B | 股票代號 | String | Text | 2330 |
| C | 股票名稱 | String | Text | 台積電 |
| D | 股數 | Number | #,##0 | 1,234,567 |
| E | 權重(%) | Number | 0.0000 | 12.3456 |

### 7.2 資料範例

```
| 日期       | 股票代號 | 股票名稱 | 股數      | 權重(%)  |
|------------|----------|----------|-----------|----------|
| 2025-11-26 | 2330     | 台積電   | 1,234,567 | 12.3456  |
| 2025-11-26 | 2317     | 鴻海     | 987,654   | 8.7654   |
| 2025-11-25 | 2330     | 台積電   | 1,230,000 | 12.3000  |
| 2025-11-25 | 2317     | 鴻海     | 990,000   | 8.8000   |
```

---

## 8. 實體關係圖 (ERD)

```
+------------------+         +-------------------+
|  DailySnapshot   |         |     Holding       |
+------------------+         +-------------------+
| - date: LocalDate| 1    * | - stockCode: String|
| - holdings: List |-------->| - stockName: String|
| - totalCount: int|         | - shares: Long     |
| - totalWeight    |         | - weight: BigDecimal|
+------------------+         +-------------------+
                                    |
                                    | compare
                                    v
                            +-------------------+
                            |  HoldingChange    |
                            +-------------------+
                            | - stockCode       |
                            | - stockName       |
                            | - changeType      |
                            | - startShares     |
                            | - endShares       |
                            | - sharesDiff      |
                            | - changeRatio     |
                            | - startWeight     |
                            | - endWeight       |
                            | - weightDiff      |
                            +-------------------+
```

---

## 9. 索引與查詢最佳化

由於使用 Excel 檔案儲存，無法使用傳統資料庫索引。採用以下策略：

### 9.1 記憶體快取策略

```java
/**
 * 快取最近載入的快照資料
 * 使用 LRU 策略，最多快取 90 天資料
 */
private final Map<LocalDate, DailySnapshot> snapshotCache = 
    new LinkedHashMap<>(100, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<LocalDate, DailySnapshot> eldest) {
            return size() > 90;
        }
    };
```

### 9.2 查詢效率考量

| 操作 | 時間複雜度 | 說明 |
|------|------------|------|
| 載入單日資料 | O(n) | n = 總記錄數，需掃描 Excel |
| 快取命中查詢 | O(1) | 直接從快取取得 |
| 區間比較 | O(m + n) | m, n = 兩日成分股數量 |
| 90 天清理 | O(n) | n = 總記錄數 |
