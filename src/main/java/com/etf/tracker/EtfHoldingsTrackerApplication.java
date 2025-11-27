package com.etf.tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ETF Holdings Tracker 應用程式主入口點
 * 
 * <p>
 * 此類別為 Spring Boot 應用程式的入口點。
 * 應用程式整合 Spring Boot 與 JavaFX，用於追蹤 ETF 00981A 的每日持倉變化。
 * </p>
 * 
 * @author ETF Tracker Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableScheduling
public class EtfHoldingsTrackerApplication {

    /**
     * 應用程式主方法
     * 
     * @param args 命令列參數
     */
    public static void main(String[] args) {
        SpringApplication.run(EtfHoldingsTrackerApplication.class, args);
    }
}
