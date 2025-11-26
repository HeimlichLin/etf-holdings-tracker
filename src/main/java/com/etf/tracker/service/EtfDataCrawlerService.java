package com.etf.tracker.service;

import com.etf.tracker.dto.HoldingDto;

import java.util.List;

/**
 * ETF 資料爬取服務介面
 * 負責從外部來源獲取 ETF 成分股資料
 */
public interface EtfDataCrawlerService {

    /**
     * 爬取指定 ETF 的成分股資料
     * 
     * @param etfSymbol ETF 代碼
     * @return 成分股列表
     */
    List<HoldingDto> crawlHoldings(String etfSymbol);

    /**
     * 檢查該 ETF 是否支援爬取
     * 
     * @param etfSymbol ETF 代碼
     * @return 是否支援
     */
    boolean isSupported(String etfSymbol);
}
