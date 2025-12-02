package com.etf.tracker.gui;

import java.io.IOException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;

/**
 * JavaFX 主應用程式
 * <p>
 * 整合 Spring Boot 與 JavaFX 的應用程式入口點
 * 支援響應式佈局、鍵盤快捷鍵、視窗大小變更監聽
 * </p>
 *
 * @author ETF Tracker Team
 * @version 2.0.0
 */
public class MainApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

    private static final String APP_TITLE = "ETF 00981A 持倉追蹤器";
    private static final int DEFAULT_WIDTH = 960;
    private static final int DEFAULT_HEIGHT = 720;
    private static final int MIN_WIDTH = 800;
    private static final int MIN_HEIGHT = 600;
    private static final int COMPACT_MODE_THRESHOLD = 900;

    private ConfigurableApplicationContext springContext;
    private MainViewController mainController;
    private Scene scene;
    private Stage primaryStage;

    @Override
    public void init() {
        logger.info("初始化 Spring Boot 應用程式...");

        ApplicationContextInitializer<GenericApplicationContext> initializer = applicationContext -> applicationContext
                .registerBean(
                        Application.class, () -> MainApp.this);

        springContext = new SpringApplicationBuilder()
                .sources(com.etf.tracker.EtfHoldingsTrackerApplication.class)
                .initializers(initializer)
                .run(getParameters().getRaw().toArray(new String[0]));

        logger.info("Spring Boot 應用程式初始化完成");
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        logger.info("啟動 JavaFX 應用程式...");

        try {
            // 載入 FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            loader.setControllerFactory(springContext::getBean);

            Parent root = loader.load();
            mainController = loader.getController();

            // 設定場景
            scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);

            // 載入 CSS
            String cssPath = Objects.requireNonNull(getClass().getResource("/css/styles.css")).toExternalForm();
            scene.getStylesheets().add(cssPath);

            // 設定視窗
            primaryStage.setTitle(APP_TITLE);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(MIN_WIDTH);
            primaryStage.setMinHeight(MIN_HEIGHT);

            // 載入圖示
            loadApplicationIcon(primaryStage);

            // 設定視窗大小變更監聽器
            setupWindowResizeListener();

            // 設定鍵盤快捷鍵
            setupKeyboardShortcuts();

            // 視窗關閉時停止應用程式
            primaryStage.setOnCloseRequest(event -> {
                logger.info("關閉應用程式");
                Platform.exit();
            });

            primaryStage.show();
            logger.info("應用程式啟動完成");

        } catch (IOException e) {
            logger.error("載入 FXML 失敗: {}", e.getMessage(), e);
            showError("應用程式啟動失敗", e.getMessage());
            Platform.exit();
        }
    }

    /**
     * 載入應用程式圖示
     */
    private void loadApplicationIcon(Stage stage) {
        try {
            // 嘗試載入 PNG 圖示
            var iconStream = getClass().getResourceAsStream("/images/app-icon.png");
            if (iconStream == null) {
                // 嘗試備用路徑
                iconStream = getClass().getResourceAsStream("/images/icon.png");
            }
            if (iconStream != null) {
                Image icon = new Image(iconStream);
                stage.getIcons().add(icon);
                logger.debug("應用程式圖示載入成功");
            } else {
                logger.warn("找不到應用程式圖示檔案");
            }
        } catch (Exception e) {
            logger.warn("無法載入應用程式圖示: {}", e.getMessage());
        }
    }

    /**
     * 設定視窗大小變更監聽器
     * <p>
     * 根據視窗寬度自動切換 compact 模式
     * </p>
     */
    private void setupWindowResizeListener() {
        // 監聽視窗寬度變化
        primaryStage.widthProperty().addListener((observable, oldValue, newValue) -> {
            double width = newValue.doubleValue();
            updateLayoutMode(width);
        });

        // 監聽視窗高度變化（可選，用於日誌記錄）
        primaryStage.heightProperty().addListener((observable, oldValue, newValue) -> {
            logger.debug("視窗高度變更: {} -> {}", oldValue, newValue);
        });

        // 初始化時檢查當前寬度
        updateLayoutMode(primaryStage.getWidth());
    }

    /**
     * 根據視窗寬度更新佈局模式
     */
    private void updateLayoutMode(double width) {
        Platform.runLater(() -> {
            Parent root = scene.getRoot();
            if (width < COMPACT_MODE_THRESHOLD) {
                // 切換到緊湊模式
                if (!root.getStyleClass().contains("compact-mode")) {
                    root.getStyleClass().add("compact-mode");
                    logger.debug("切換到緊湊模式 (寬度: {})", width);
                }
            } else {
                // 切換到標準模式
                root.getStyleClass().remove("compact-mode");
                logger.debug("切換到標準模式 (寬度: {})", width);
            }
        });
    }

    /**
     * 設定鍵盤快捷鍵
     */
    private void setupKeyboardShortcuts() {
        scene.setOnKeyPressed(event -> {
            // F5: 重新整理/抓取資料
            if (event.getCode() == KeyCode.F5) {
                logger.debug("按下 F5 快捷鍵");
                if (mainController != null) {
                    mainController.handleRefresh();
                }
                event.consume();
            }

            // Ctrl+Q: 離開應用程式
            KeyCombination ctrlQ = new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN);
            if (ctrlQ.match(event)) {
                logger.info("按下 Ctrl+Q 快捷鍵 - 離開應用程式");
                Platform.exit();
                event.consume();
            }

            // Ctrl+R: 重新載入資料
            KeyCombination ctrlR = new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN);
            if (ctrlR.match(event)) {
                logger.debug("按下 Ctrl+R 快捷鍵");
                if (mainController != null) {
                    mainController.handleRefresh();
                }
                event.consume();
            }

            // Ctrl+F: 焦點移至搜尋框
            KeyCombination ctrlF = new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN);
            if (ctrlF.match(event)) {
                logger.debug("按下 Ctrl+F 快捷鍵");
                if (mainController != null) {
                    mainController.focusSearchField();
                }
                event.consume();
            }

            // ESC: 清除搜尋或返回
            if (event.getCode() == KeyCode.ESCAPE) {
                logger.debug("按下 ESC 快捷鍵");
                if (mainController != null) {
                    mainController.handleEscape();
                }
                event.consume();
            }

            // F1: 顯示快捷鍵說明（可選）
            if (event.getCode() == KeyCode.F1) {
                logger.debug("按下 F1 快捷鍵");
                showKeyboardShortcutsHelp();
                event.consume();
            }
        });
    }

    /**
     * 顯示鍵盤快捷鍵說明
     */
    private void showKeyboardShortcutsHelp() {
        String shortcuts = """
                鍵盤快捷鍵:

                F5          - 重新整理資料
                Ctrl+R      - 重新整理資料
                Ctrl+F      - 搜尋
                Ctrl+Q      - 離開應用程式
                ESC         - 清除搜尋/返回
                F1          - 顯示此說明
                """;

        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("鍵盤快捷鍵");
        alert.setHeaderText("快捷鍵說明");
        alert.setContentText(shortcuts);
        alert.showAndWait();
    }

    @Override
    public void stop() {
        logger.info("停止應用程式...");
        if (springContext != null) {
            springContext.close();
        }
        logger.info("應用程式已停止");
    }

    /**
     * 顯示錯誤對話框
     */
    private void showError(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 取得 Spring Context
     * 
     * @return Spring 應用程式上下文
     */
    public ConfigurableApplicationContext getSpringContext() {
        return springContext;
    }

    /**
     * 取得主舞台
     * 
     * @return 主視窗舞台
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * 應用程式入口點
     */
    public static void main(String[] args) {
        launch(args);
    }
}
