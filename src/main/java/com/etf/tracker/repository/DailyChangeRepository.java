package com.etf.tracker.repository;

import com.etf.tracker.model.DailyChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * ETF 每日變動資料存取層
 */
@Repository
public interface DailyChangeRepository extends JpaRepository<DailyChange, Long> {

    /**
     * 根據 ETF 代碼和日期查詢變動記錄
     */
    List<DailyChange> findByEtfSymbolAndChangeDate(String etfSymbol, LocalDate changeDate);

    /**
     * 根據 ETF 代碼查詢指定日期範圍內的變動記錄
     */
    List<DailyChange> findByEtfSymbolAndChangeDateBetweenOrderByChangeDateDesc(
            String etfSymbol, LocalDate startDate, LocalDate endDate);

    /**
     * 根據變動類型查詢
     */
    List<DailyChange> findByEtfSymbolAndChangeType(String etfSymbol, DailyChange.ChangeType changeType);

    /**
     * 查詢特定股票的變動歷史
     */
    List<DailyChange> findByEtfSymbolAndStockSymbolOrderByChangeDateDesc(
            String etfSymbol, String stockSymbol);

    /**
     * 刪除超過指定日期的舊資料
     */
    @Modifying
    @Query("DELETE FROM DailyChange d WHERE d.changeDate < :cutoffDate")
    void deleteOlderThan(LocalDate cutoffDate);

    /**
     * 查詢最近 N 天的變動統計
     */
    @Query("SELECT d.changeType, COUNT(d) FROM DailyChange d " +
           "WHERE d.etfSymbol = :etfSymbol AND d.changeDate >= :startDate " +
           "GROUP BY d.changeType")
    List<Object[]> countChangesByType(String etfSymbol, LocalDate startDate);
}
