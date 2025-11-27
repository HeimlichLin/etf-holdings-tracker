package com.etf.tracker.dto.mapper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.etf.tracker.dto.DailySnapshotDto;
import com.etf.tracker.model.DailySnapshot;

/**
 * 每日快照手動映射器
 * <p>
 * 提供 Entity ↔ DTO 轉換方法
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
public final class DailySnapshotMapper {

    private DailySnapshotMapper() {
        // 禁止實例化
    }

    /**
     * Entity → DTO
     *
     * @param entity DailySnapshot 實體
     * @return DailySnapshotDto，若 entity 為 null 則返回 null
     */
    public static DailySnapshotDto toDto(DailySnapshot entity) {
        if (entity == null) {
            return null;
        }
        return new DailySnapshotDto(
                entity.getDate(),
                HoldingMapper.toDtoList(entity.getHoldings()),
                entity.getTotalCount(),
                entity.getTotalWeight());
    }

    /**
     * DTO → Entity
     *
     * @param dto DailySnapshotDto 資料傳輸物件
     * @return DailySnapshot 實體，若 dto 為 null 則返回 null
     */
    public static DailySnapshot toEntity(DailySnapshotDto dto) {
        if (dto == null) {
            return null;
        }
        return DailySnapshot.builder()
                .date(dto.date())
                .holdings(HoldingMapper.toEntityList(dto.holdings()))
                .totalCount(dto.totalCount())
                .totalWeight(dto.totalWeight())
                .build();
    }

    /**
     * Entity List → DTO List
     *
     * @param entities DailySnapshot 實體清單
     * @return DailySnapshotDto 清單，若 entities 為 null 則返回空清單
     */
    public static List<DailySnapshotDto> toDtoList(List<DailySnapshot> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream()
                .map(DailySnapshotMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * DTO List → Entity List
     *
     * @param dtos DailySnapshotDto 清單
     * @return DailySnapshot 實體清單，若 dtos 為 null 則返回空清單
     */
    public static List<DailySnapshot> toEntityList(List<DailySnapshotDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return Collections.emptyList();
        }
        return dtos.stream()
                .map(DailySnapshotMapper::toEntity)
                .collect(Collectors.toList());
    }
}
