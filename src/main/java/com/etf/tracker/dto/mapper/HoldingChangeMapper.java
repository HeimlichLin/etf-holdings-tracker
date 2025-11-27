package com.etf.tracker.dto.mapper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.etf.tracker.dto.HoldingChangeDto;
import com.etf.tracker.model.HoldingChange;

/**
 * 持倉變化手動映射器
 * <p>
 * 提供 Entity ↔ DTO 轉換方法
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
public final class HoldingChangeMapper {

    private HoldingChangeMapper() {
        // 禁止實例化
    }

    /**
     * Entity → DTO
     *
     * @param entity HoldingChange 實體
     * @return HoldingChangeDto，若 entity 為 null 則返回 null
     */
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
                entity.getWeightDiff());
    }

    /**
     * DTO → Entity
     *
     * @param dto HoldingChangeDto 資料傳輸物件
     * @return HoldingChange 實體，若 dto 為 null 則返回 null
     */
    public static HoldingChange toEntity(HoldingChangeDto dto) {
        if (dto == null) {
            return null;
        }
        return HoldingChange.builder()
                .stockCode(dto.stockCode())
                .stockName(dto.stockName())
                .changeType(dto.changeType())
                .startShares(dto.startShares())
                .endShares(dto.endShares())
                .sharesDiff(dto.sharesDiff())
                .changeRatio(dto.changeRatio())
                .startWeight(dto.startWeight())
                .endWeight(dto.endWeight())
                .weightDiff(dto.weightDiff())
                .build();
    }

    /**
     * Entity List → DTO List
     *
     * @param entities HoldingChange 實體清單
     * @return HoldingChangeDto 清單，若 entities 為 null 則返回空清單
     */
    public static List<HoldingChangeDto> toDtoList(List<HoldingChange> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream()
                .map(HoldingChangeMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * DTO List → Entity List
     *
     * @param dtos HoldingChangeDto 清單
     * @return HoldingChange 實體清單，若 dtos 為 null 則返回空清單
     */
    public static List<HoldingChange> toEntityList(List<HoldingChangeDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return Collections.emptyList();
        }
        return dtos.stream()
                .map(HoldingChangeMapper::toEntity)
                .collect(Collectors.toList());
    }
}
