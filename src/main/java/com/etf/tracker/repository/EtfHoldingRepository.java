package com.etf.tracker.repository;

import com.etf.tracker.model.EtfHolding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ETF 成分股資料存取層
 */
@Repository
public interface EtfHoldingRepository extends JpaRepository<EtfHolding, Long> {

    /**
     * 根據 ETF 代碼和日期查詢成分股
     */
    List<EtfHolding> findByEtfSymbolAndRecordDate(String etfSymbol, LocalDate recordDate);

    /**
     * 查詢特定 ETF 最新的記錄日期
     */
    @Query("SELECT MAX(e.recordDate) FROM EtfHolding e WHERE e.etfSymbol = :etfSymbol")
    Optional<LocalDate> findLatestRecordDate(String etfSymbol);

    /**
     * 根據 ETF 代碼、日期和股票代碼查詢
     */
    Optional<EtfHolding> findByEtfSymbolAndRecordDateAndStockSymbol(
            String etfSymbol, LocalDate recordDate, String stockSymbol);

    /**
     * 刪除超過指定日期的舊資料 (用於清理超過一季的資料)
     */
    @Modifying
    @Query("DELETE FROM EtfHolding e WHERE e.recordDate < :cutoffDate")
    void deleteOlderThan(LocalDate cutoffDate);

    /**
     * 查詢所有不重複的 ETF 代碼
     */
    @Query("SELECT DISTINCT e.etfSymbol FROM EtfHolding e")
    List<String> findDistinctEtfSymbols();
}
