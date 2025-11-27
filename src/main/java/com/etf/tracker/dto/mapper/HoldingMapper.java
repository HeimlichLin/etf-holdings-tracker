package com.etf.tracker.dto.mapper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.etf.tracker.dto.HoldingDto;
import com.etf.tracker.model.Holding;

/**
 * 成分股手動映射器
 * <p>
 * 提供 Entity ↔ DTO 轉換方法
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
public final class HoldingMapper {

    private HoldingMapper() {
        // 禁止實例化
    }

    /**
     * Entity → DTO
     *
     * @param entity Holding 實體
     * @return HoldingDto，若 entity 為 null 則返回 null
     */
    public static HoldingDto toDto(Holding entity) {
        if (entity == null) {
            return null;
        }
        return new HoldingDto(
                entity.getStockCode(),
                entity.getStockName(),
                entity.getShares(),
                entity.getWeight());
    }

    /**
     * DTO → Entity
     *
     * @param dto HoldingDto 資料傳輸物件
     * @return Holding 實體，若 dto 為 null 則返回 null
     */
    public static Holding toEntity(HoldingDto dto) {
        if (dto == null) {
            return null;
        }
        return Holding.builder()
                .stockCode(dto.stockCode())
                .stockName(dto.stockName())
                .shares(dto.shares())
                .weight(dto.weight())
                .build();
    }

    /**
     * Entity List → DTO List
     *
     * @param entities Holding 實體清單
     * @return HoldingDto 清單，若 entities 為 null 則返回空清單
     */
    public static List<HoldingDto> toDtoList(List<Holding> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream()
                .map(HoldingMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * DTO List → Entity List
     *
     * @param dtos HoldingDto 清單
     * @return Holding 實體清單，若 dtos 為 null 則返回空清單
     */
    public static List<Holding> toEntityList(List<HoldingDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return Collections.emptyList();
        }
        return dtos.stream()
                .map(HoldingMapper::toEntity)
                .collect(Collectors.toList());
    }
}
