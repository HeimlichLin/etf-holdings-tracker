package com.etf.tracker.gui;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.etf.tracker.dto.DailySnapshotDto;
import com.etf.tracker.dto.HoldingDto;
import com.etf.tracker.dto.mapper.DailySnapshotMapper;
import com.etf.tracker.gui.component.ConfirmDialog;
import com.etf.tracker.gui.view.RangeCompareViewController;
import com.etf.tracker.model.DailySnapshot;
import com.etf.tracker.service.DataCleanupService;
import com.etf.tracker.service.DataFetchService;
import com.etf.tracker.service.ExcelStorageService;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;

/**
 * 主視圖控制器
 * <p>
 * 處理 JavaFX GUI 的事件與資料綁定
 * 支援導航功能、鍵盤快捷鍵
 * </p>
 *
 * @author ETF Tracker Team
 * @version 2.0.0
 */
@Component
public class MainViewController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(MainViewController.class);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.TAIWAN);

    // 根容器
    @FXML
    private BorderPane rootPane;

    // 導航按鈕
    @FXML
    private Button navHomeButton;
    @FXML
    private Button navCompareButton;

    // 工具列元件
    @FXML
    private Button fetchButton;
    @FXML
    private Button viewButton;
    @FXML
    private Button refreshButton;
    @FXML
    private DatePicker datePicker;
    @FXML
    private Label statusLabel;

    // 摘要區
    @FXML
    private Label dataDateLabel;
    @FXML
    private Label stockCountLabel;
    @FXML
    private Label totalWeightLabel;
    @FXML
    private Label lastUpdateLabel;

    // 資料表格
    @FXML
    private TableView<HoldingDto> holdingsTable;
    @FXML
    private TableColumn<HoldingDto, String> stockCodeColumn;
    @FXML
    private TableColumn<HoldingDto, String> stockNameColumn;
    @FXML
    private TableColumn<HoldingDto, String> sharesColumn;
    @FXML
    private TableColumn<HoldingDto, String> weightColumn;

    // 搜尋與狀態
    @FXML
    private TextField searchField;
    @FXML
    private Label recordCountLabel;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Label progressLabel;
    @FXML
    private Label versionLabel;

    // 服務
    private final DataFetchService dataFetchService;
    private final ExcelStorageService excelStorageService;
    private final DataCleanupService dataCleanupService;
    private final ApplicationContext applicationContext;

    // 資料
    private final ObservableList<HoldingDto> holdingsData = FXCollections.observableArrayList();
    private FilteredList<HoldingDto> filteredData;

    // 當前導航狀態
    private String currentView = "home";
    private Node homeView;

    public MainViewController(DataFetchService dataFetchService,
            ExcelStorageService excelStorageService,
            DataCleanupService dataCleanupService,
            ApplicationContext applicationContext) {
        this.dataFetchService = dataFetchService;
        this.excelStorageService = excelStorageService;
        this.dataCleanupService = dataCleanupService;
        this.applicationContext = applicationContext;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("初始化主視圖控制器");

        // 檢查導航按鈕是否正確注入
        if (navCompareButton == null) {
            logger.error("嚴重錯誤: navCompareButton 未注入！");
        } else {
            logger.info("navCompareButton 已注入，可見性: {}", navCompareButton.isVisible());
        }

        // 保存主畫面視圖
        if (rootPane != null) {
            homeView = rootPane.getCenter();
        }

        setupTableColumns();
        setupSearchFilter();
        loadAvailableDates();
        loadLatestData();

        logger.info("主視圖控制器初始化完成");
    }

    /**
     * 設定表格欄位
     */
    private void setupTableColumns() {
        // 股票代號 - 使用 Lambda 表達式支援 Record
        stockCodeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().stockCode()));

        // 股票名稱 - 使用 Lambda 表達式支援 Record
        stockNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().stockName()));

        // 股數格式化
        sharesColumn.setCellValueFactory(data -> {
            Long shares = data.getValue().shares();
            return new SimpleStringProperty(NUMBER_FORMAT.format(shares));
        });
        sharesColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        // 權重格式化
        weightColumn.setCellValueFactory(data -> {
            BigDecimal weight = data.getValue().weight();
            return new SimpleStringProperty(String.format("%.2f%%", weight));
        });
        weightColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        // 設定表格資料
        filteredData = new FilteredList<>(holdingsData, p -> true);
        holdingsTable.setItems(filteredData);
    }

    /**
     * 設定搜尋過濾
     */
    private void setupSearchFilter() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(holding -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return holding.stockCode().toLowerCase().contains(lowerCaseFilter) ||
                        holding.stockName().toLowerCase().contains(lowerCaseFilter);
            });
            updateRecordCount();
        });
    }

    /**
     * 載入可用日期
     */
    private void loadAvailableDates() {
        List<LocalDate> dates = excelStorageService.getAvailableDates();
        if (!dates.isEmpty()) {
            datePicker.setValue(dates.get(0));
        }
    }

    /**
     * 載入最新資料
     */
    private void loadLatestData() {
        Optional<DailySnapshot> snapshot = excelStorageService.getLatestSnapshot();
        snapshot.ifPresent(this::displaySnapshot);
    }

    /**
     * 處理抓取資料按鈕
     */
    @FXML
    private void handleFetchData() {
        logger.info("使用者點擊抓取資料");
        setLoading(true, "正在抓取資料...");

        Task<DailySnapshot> task = new Task<>() {
            @Override
            protected DailySnapshot call() throws Exception {
                DailySnapshot snapshot = dataFetchService.fetchLatestHoldings();
                excelStorageService.saveSnapshot(snapshot);
                return snapshot;
            }
        };

        task.setOnSucceeded(event -> {
            DailySnapshot snapshot = task.getValue();
            displaySnapshot(snapshot);
            setLoading(false, "抓取完成");
            showInfo("資料抓取成功", String.format("已取得 %d 筆持倉資料", snapshot.getTotalCount()));
            loadAvailableDates();
        });

        task.setOnFailed(event -> {
            Throwable error = task.getException();
            logger.error("抓取資料失敗: {}", error.getMessage(), error);
            setLoading(false, "抓取失敗");
            showError("資料抓取失敗", error.getMessage());
        });

        new Thread(task).start();
    }

    /**
     * 處理日期選擇
     */
    @FXML
    private void handleDateSelected() {
        // 日期選擇事件
    }

    /**
     * 處理檢視資料按鈕
     */
    @FXML
    private void handleViewData() {
        LocalDate selectedDate = datePicker.getValue();
        if (selectedDate == null) {
            showWarning("請選擇日期", "請先選擇要檢視的日期");
            return;
        }

        logger.info("檢視 {} 的資料", selectedDate);
        setLoading(true, "載入資料中...");

        Task<Optional<DailySnapshot>> task = new Task<>() {
            @Override
            protected Optional<DailySnapshot> call() {
                return excelStorageService.getSnapshot(selectedDate);
            }
        };

        task.setOnSucceeded(event -> {
            Optional<DailySnapshot> snapshot = task.getValue();
            if (snapshot.isPresent()) {
                displaySnapshot(snapshot.get());
                setLoading(false, "載入完成");
            } else {
                setLoading(false, "無資料");
                showWarning("查無資料", String.format("找不到 %s 的持倉資料", selectedDate));
            }
        });

        task.setOnFailed(event -> {
            logger.error("載入資料失敗: {}", task.getException().getMessage());
            setLoading(false, "載入失敗");
            showError("載入失敗", task.getException().getMessage());
        });

        new Thread(task).start();
    }

    /**
     * 處理重新整理按鈕
     */
    @FXML
    public void handleRefresh() {
        loadAvailableDates();
        loadLatestData();
        statusLabel.setText("已重新整理");
    }

    /**
     * 處理搜尋
     */
    @FXML
    private void handleSearch() {
        updateRecordCount();
    }

    /**
     * 處理清除搜尋
     */
    @FXML
    private void handleClearSearch() {
        searchField.clear();
    }

    /**
     * 處理清理舊資料
     */
    @FXML
    private void handleCleanup() {
        // 取得預設保留天數
        int defaultDays = dataCleanupService.getDefaultRetentionDays();

        // 使用 ConfirmDialog.CleanupConfirmDialog 顯示確認對話框
        // 傳入 ExcelStorageService 以便動態計算實際 Excel 中的記錄數
        ConfirmDialog.CleanupConfirmDialog dialog = new ConfirmDialog.CleanupConfirmDialog(defaultDays,
                excelStorageService);

        dialog.showAndWait().ifPresent(result -> {
            if (result.confirmed()) {
                performCleanup(result.daysToKeep());
            }
        });
    }

    /**
     * 執行資料清理
     *
     * @param daysToKeep 保留天數
     */
    private void performCleanup(int daysToKeep) {
        setLoading(true, "正在清理資料...");

        Task<com.etf.tracker.dto.CleanupResultDto> task = new Task<>() {
            @Override
            protected com.etf.tracker.dto.CleanupResultDto call() {
                return dataCleanupService.cleanupOldData(daysToKeep);
            }
        };

        task.setOnSucceeded(event -> {
            com.etf.tracker.dto.CleanupResultDto result = task.getValue();
            setLoading(false, "清理完成");

            if (result.success()) {
                showInfo("清理完成", result.message());
                loadAvailableDates();
                loadLatestData();
            } else {
                showError("清理失敗", result.message());
            }
        });

        task.setOnFailed(event -> {
            logger.error("清理資料失敗: {}", task.getException().getMessage());
            setLoading(false, "清理失敗");
            showError("清理失敗", task.getException().getMessage());
        });

        new Thread(task).start();
    }

    /**
     * 顯示快照資料
     */
    private void displaySnapshot(DailySnapshot snapshot) {
        Platform.runLater(() -> {
            DailySnapshotDto dto = DailySnapshotMapper.toDto(snapshot);

            // 更新摘要
            dataDateLabel.setText(dto.date().format(DATE_FORMATTER));
            stockCountLabel.setText(String.valueOf(dto.totalCount()));
            totalWeightLabel.setText(String.format("%.2f%%", dto.totalWeight()));
            lastUpdateLabel.setText(LocalDateTime.now().format(DATETIME_FORMATTER));

            // 更新表格
            holdingsData.clear();
            holdingsData.addAll(dto.holdings());
            updateRecordCount();

            // 更新日期選擇器
            datePicker.setValue(dto.date());
        });
    }

    /**
     * 更新記錄數量
     */
    private void updateRecordCount() {
        int total = holdingsData.size();
        int filtered = filteredData.size();
        if (total == filtered) {
            recordCountLabel.setText(String.format("共 %d 筆資料", total));
        } else {
            recordCountLabel.setText(String.format("顯示 %d / 共 %d 筆資料", filtered, total));
        }
    }

    /**
     * 設定載入狀態
     */
    private void setLoading(boolean loading, String message) {
        Platform.runLater(() -> {
            fetchButton.setDisable(loading);
            viewButton.setDisable(loading);
            refreshButton.setDisable(loading);
            progressIndicator.setVisible(loading);
            progressLabel.setText(message);
            statusLabel.setText(loading ? "處理中..." : "就緒");
        });
    }

    /**
     * 顯示資訊對話框
     */
    private void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * 顯示警告對話框
     */
    private void showWarning(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * 顯示錯誤對話框
     */
    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // ========== 導航功能 ==========

    /**
     * 處理導航到主畫面
     */
    @FXML
    public void handleNavHome() {
        logger.info("導航到主畫面");
        updateNavigationState("home");
        if (homeView != null) {
            rootPane.setCenter(homeView);
        }
    }

    /**
     * 處理導航到區間比較
     */
    @FXML
    public void handleNavCompare() {
        logger.info("導航到區間比較");
        updateNavigationState("compare");

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/range-compare.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent view = loader.load();

            RangeCompareViewController controller = loader.getController();
            controller.setBackHandler(this::handleNavHome);

            rootPane.setCenter(view);
        } catch (IOException e) {
            logger.error("無法載入區間比較視圖", e);
            showError("錯誤", "無法載入區間比較視圖: " + e.getMessage());
        }
    }

    /**
     * 更新導航按鈕狀態
     */
    private void updateNavigationState(String viewName) {
        this.currentView = viewName;
        Platform.runLater(() -> {
            // 移除所有按鈕的 active 狀態
            if (navHomeButton != null) {
                navHomeButton.getStyleClass().remove("nav-button-active");
            }
            if (navCompareButton != null) {
                navCompareButton.getStyleClass().remove("nav-button-active");
            }

            // 設定當前按鈕的 active 狀態
            switch (viewName) {
                case "home":
                    if (navHomeButton != null) {
                        navHomeButton.getStyleClass().add("nav-button-active");
                    }
                    break;
                case "compare":
                    if (navCompareButton != null) {
                        navCompareButton.getStyleClass().add("nav-button-active");
                    }
                    break;
                default:
                    break;
            }
        });
    }

    // ========== 快捷鍵支援 ==========

    /**
     * 處理 ESC 鍵
     * 清除搜尋框或返回
     */
    public void handleEscape() {
        if (searchField != null && !searchField.getText().isEmpty()) {
            handleClearSearch();
            logger.debug("ESC: 清除搜尋");
        } else {
            // 如果搜尋框為空，可以考慮其他動作
            logger.debug("ESC: 無動作（搜尋框已清空）");
        }
    }

    /**
     * 焦點移至搜尋框
     */
    public void focusSearchField() {
        if (searchField != null) {
            searchField.requestFocus();
            searchField.selectAll();
            logger.debug("焦點移至搜尋框");
        }
    }

    /**
     * 取得當前視圖名稱
     * 
     * @return 當前視圖名稱
     */
    public String getCurrentView() {
        return currentView;
    }
}
