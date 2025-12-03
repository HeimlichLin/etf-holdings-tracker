/**
 * ETF Holdings Tracker 模組定義
 * 
 * <p>
 * 定義應用程式所需的模組依賴，用於 jlink 打包成原生應用程式。
 * </p>
 */
module com.etf.tracker {
    // Java 標準模組
    requires java.base;
    requires java.logging;
    requires java.sql;
    requires java.desktop;
    requires java.instrument;
    requires java.net.http;

    // Spring Framework 模組
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.core;
    requires spring.beans;
    requires spring.web;
    requires spring.webmvc;

    // JavaFX 模組
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;

    // 第三方函式庫
    requires okhttp3;
    requires org.jsoup;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires org.slf4j;
    requires playwright;

    // JSON 解析 (Google Sheets 公開 API 使用)
    requires com.google.gson;

    // Jakarta Annotation
    requires jakarta.annotation;

    // 開放套件給 Spring 反射存取
    opens com.etf.tracker to spring.core, spring.beans, spring.context;
    opens com.etf.tracker.config to spring.core, spring.beans, spring.context;
    opens com.etf.tracker.model to com.fasterxml.jackson.databind, spring.core;
    opens com.etf.tracker.dto to com.fasterxml.jackson.databind, spring.core;
    opens com.etf.tracker.dto.mapper to spring.beans, spring.core;
    opens com.etf.tracker.exception to spring.web, spring.beans, spring.core;
    opens com.etf.tracker.controller to spring.web, spring.beans, spring.context, spring.core;
    opens com.etf.tracker.service to spring.beans, spring.context, spring.core;
    opens com.etf.tracker.scraper to spring.beans, spring.context, spring.core;
    opens com.etf.tracker.gui to javafx.fxml, spring.core, spring.beans, spring.context;
    opens com.etf.tracker.gui.view to javafx.fxml, spring.core, spring.beans, spring.context;
    opens com.etf.tracker.gui.component to javafx.fxml, spring.core, spring.beans, spring.context;

    // 匯出公開 API
    exports com.etf.tracker;
    exports com.etf.tracker.model;
    exports com.etf.tracker.dto;
    exports com.etf.tracker.dto.mapper;
    exports com.etf.tracker.exception;
    exports com.etf.tracker.config;
    exports com.etf.tracker.service;
    exports com.etf.tracker.controller;
    exports com.etf.tracker.scraper;
    exports com.etf.tracker.gui;
    exports com.etf.tracker.gui.view;
    exports com.etf.tracker.gui.component;
}
