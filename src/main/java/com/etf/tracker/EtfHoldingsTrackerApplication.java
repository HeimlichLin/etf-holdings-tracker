package com.etf.tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ETF Holdings Tracker Application
 * 追蹤特定主動型 ETF 的成分股每日變化
 */
@SpringBootApplication
@EnableScheduling
public class EtfHoldingsTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EtfHoldingsTrackerApplication.class, args);
    }
}
