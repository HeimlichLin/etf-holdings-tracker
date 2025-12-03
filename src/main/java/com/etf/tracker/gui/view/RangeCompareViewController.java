package com.etf.tracker.gui.view;

import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.etf.tracker.dto.HoldingChangeDto;
import com.etf.tracker.dto.RangeCompareResultDto;
import com.etf.tracker.service.HoldingCompareService;
import com.etf.tracker.service.StorageService;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

/**
 * 區間比較視圖控制器
 * <p>
 * 處理兩個日期間持倉變化的比較與顯示，包括：
 * <ul>
 * <li>新進增持</li>
 * <li>剔除減持</li>
 * <li>增持</li>
 * <li>減持</li>
 * <li>不變</li>
 * </ul>
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
@Component
public class RangeCompareViewController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(RangeCompareViewController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.TAIWAN);

    // 根容器
    @FXML
    private BorderPane rootPane;

    // 日期選擇
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private Button compareButton;
    @FXML
    private Label statusLabel;

    // 摘要資訊
    @FXML
    private Label startDateLabel;
    @FXML
    private Label endDateLabel;
    @FXML
    private Label newAdditionsLabel;
    @FXML
    private Label removalsLabel;
    @FXML
    private Label increasedLabel;
    @FXML
    private Label decreasedLabel;
    @FXML
    private Label unchangedLabel;

    // 分類標籤頁
    @FXML
    private TabPane categoryTabPane;

    // 新進增持表格
    @FXML
    private TableView<HoldingChangeDto> newAdditionsTable;
    @FXML
    private TableColumn<HoldingChangeDto, String> naStockCodeColumn;
    @FXML
    private TableColumn<HoldingChangeDto, String> naStockNameColumn;
    @FXML
    private TableColumn<HoldingChangeDto, String> naEndSharesColumn;
    @FXML
    private TableColumn<HoldingChangeDto, String> naEndWeightColumn;

    // 剔除減持表格
    @FXML
    private TableView<HoldingChangeDto> removalsTable;
    @FXML
    private TableColumn<HoldingChangeDto, String> rmStockCodeColumn;
    @FXML
    private TableColumn<HoldingChangeDto, String> rmStockNameColumn;
    @FXML
    private TableColumn<HoldingChangeDto, String> rmStartSharesColumn;
    @FXML
    private TableColumn<HoldingChangeDto, String> rmStartWeightColumn;

    // 增持表格
    @FXML
    private TableView<HoldingChangeDto> increasedTable;
    @FXML
    private TableColumn<HoldingChangeDto, String> incStockCodeColumn;
    @FXML
    private TableColumn<HoldingChangeDto, String> incStockNameColumn;
    @FXML
    private TableColumn<HoldingChangeDto, String> incStartSharesColumn;
    @FXML
    private TableColumn<HoldingChangeDto, String> incEndSharesColumn;
    @FXML
    private TableColumn<HoldingChangeDto, String> incSharesDiffColumn;
    @FXML
    private TableColumn<HoldingChangeDto, String> incChangeRatioColumn;
    @FXML
    private TableColumn<HoldingChangeDto, String> incWeightDiffColumn;

    // 減持表格
    @FXML
    private TableView<HoldingChangeDto> decreasedTable;
    @FXML
    private TableColumn<HoldingChangeDto, String> decStockCodeColumn;
    @FXML
    private TableColumn<HoldingChangeDto, String> decStockNameColumn;
    @FXML
    private TableColumn<HoldingChangeDto, String> decStartSharesColumn;
    @FXML
    private TableColumn<HoldingChangeDto, String> decEndSharesColumn;
    @FXML
    private TableColumn<HoldingChangeDto, String> decSharesDiffColumn;
    @FXML
    private TableColumn<HoldingChangeDto, String> decChangeRatioColumn;
    @FXML
    private TableColumn<HoldingChangeDto, String> decWeightDiffColumn;

    // 不變表格
    @FXML
    private TableView<HoldingChangeDto> unchangedTable;
    @FXML
    private TableColumn<HoldingChangeDto, String> uncStockCodeColumn;
    @FXML
    private TableColumn<HoldingChangeDto, String> uncStockNameColumn;
    @FXML
    private TableColumn<HoldingChangeDto, String> uncSharesColumn;
    @FXML
    private TableColumn<HoldingChangeDto, String> uncStartWeightColumn;
    @FXML
    private TableColumn<HoldingChangeDto, String> uncEndWeightColumn;
    @FXML
    private TableColumn<HoldingChangeDto, String> uncWeightDiffColumn;

    // 底部
    @FXML
    private Label recordCountLabel;
    @FXML
    private VBox loadingOverlay;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Label progressLabel;
    @FXML
    private StackPane resultContainer;

    // 服務
    private final HoldingCompareService holdingCompareService;
    private final StorageService storageService;

    // 資料
    private final ObservableList<HoldingChangeDto> newAdditionsData = FXCollections.observableArrayList();
    private final ObservableList<HoldingChangeDto> removalsData = FXCollections.observableArrayList();
    private final ObservableList<HoldingChangeDto> increasedData = FXCollections.observableArrayList();
    private final ObservableList<HoldingChangeDto> decreasedData = FXCollections.observableArrayList();
    private final ObservableList<HoldingChangeDto> unchangedData = FXCollections.observableArrayList();

    private Runnable backHandler;
    private List<LocalDate> availableDates;

    // 各類別的計數
    private int newAdditionsCount = 0;
    private int removalsCount = 0;
    private int increasedCount = 0;
    private int decreasedCount = 0;
    private int unchangedCount = 0;

    public RangeCompareViewController(HoldingCompareService holdingCompareService,
            StorageService storageService) {
        this.holdingCompareService = holdingCompareService;
        this.storageService = storageService;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("初始化區間比較視圖");

        setupDatePickers();
        setupTableColumns();
        loadAvailableDates();
        setupKeyboardShortcuts();
        setupTabPaneListener();

        logger.info("區間比較視圖初始化完成");
    }

    /**
     * 設定標籤頁選擇監聽
     */
    private void setupTabPaneListener() {
        categoryTabPane.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            updateRecordCount(newVal.intValue());
        });
    }

    /**
     * 更新記錄計數
     */
    private void updateRecordCount(int tabIndex) {
        String displayText;
        switch (tabIndex) {
            case 0: // 新進增持
                displayText = String.format("共 %d 筆變化", newAdditionsCount);
                break;
            case 1: // 剔除減持
                displayText = String.format("共 %d 筆變化", removalsCount);
                break;
            case 2: // 增持
                displayText = String.format("共 %d 筆變化", increasedCount);
                break;
            case 3: // 減持
                displayText = String.format("共 %d 筆變化", decreasedCount);
                break;
            case 4: // 不變
                displayText = "沒有變化";
                break;
            default:
                displayText = "共 0 筆變化";
                break;
        }
        recordCountLabel.setText(displayText);
    }

    /**
     * 設定鍵盤快捷鍵
     */
    private void setupKeyboardShortcuts() {
        rootPane.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                handleBack();
                event.consume();
            } else if (event.getCode() == KeyCode.F5) {
                handleCompare();
                event.consume();
            }
        });
    }

    /**
     * 設定返回處理器
     */
    public void setBackHandler(Runnable handler) {
        this.backHandler = handler;
    }

    /**
     * 設定日期選擇器
     */
    private void setupDatePickers() {
        // 日期選擇器預設值
        LocalDate today = LocalDate.now();
        endDatePicker.setValue(today);
        startDatePicker.setValue(today.minusDays(7));
    }

    /**
     * 設定可用日期限制
     */
    private void setupDateCellFactory() {
        Callback<DatePicker, DateCell> dayCellFactory = dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);

                if (availableDates == null || !availableDates.contains(item)) {
                    setDisable(true);
                    setStyle("-fx-background-color: #f0f0f0;");
                }
            }
        };

        startDatePicker.setDayCellFactory(dayCellFactory);
        endDatePicker.setDayCellFactory(dayCellFactory);
    }

    /**
     * 設定表格欄位
     */
    private void setupTableColumns() {
        // 新進增持表格
        setupNewAdditionsTableColumns();

        // 剔除減持表格
        setupRemovalsTableColumns();

        // 增持表格
        setupIncreasedTableColumns();

        // 減持表格
        setupDecreasedTableColumns();

        // 不變表格
        setupUnchangedTableColumns();

        // 綁定資料
        newAdditionsTable.setItems(newAdditionsData);
        removalsTable.setItems(removalsData);
        increasedTable.setItems(increasedData);
        decreasedTable.setItems(decreasedData);
        unchangedTable.setItems(unchangedData);
    }

    private void setupNewAdditionsTableColumns() {
        naStockCodeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().stockCode()));
        naStockNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().stockName()));
        naEndSharesColumn
                .setCellValueFactory(data -> new SimpleStringProperty(formatShares(data.getValue().endShares())));
        naEndSharesColumn.setStyle("-fx-alignment: CENTER-RIGHT;");
        naEndWeightColumn
                .setCellValueFactory(data -> new SimpleStringProperty(formatWeight(data.getValue().endWeight())));
        naEndWeightColumn.setStyle("-fx-alignment: CENTER-RIGHT;");
    }

    private void setupRemovalsTableColumns() {
        rmStockCodeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().stockCode()));
        rmStockNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().stockName()));
        rmStartSharesColumn
                .setCellValueFactory(data -> new SimpleStringProperty(formatShares(data.getValue().startShares())));
        rmStartSharesColumn.setStyle("-fx-alignment: CENTER-RIGHT;");
        rmStartWeightColumn
                .setCellValueFactory(data -> new SimpleStringProperty(formatWeight(data.getValue().startWeight())));
        rmStartWeightColumn.setStyle("-fx-alignment: CENTER-RIGHT;");
    }

    private void setupIncreasedTableColumns() {
        incStockCodeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().stockCode()));
        incStockNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().stockName()));
        incStartSharesColumn
                .setCellValueFactory(data -> new SimpleStringProperty(formatShares(data.getValue().startShares())));
        incStartSharesColumn.setStyle("-fx-alignment: CENTER-RIGHT;");
        incEndSharesColumn
                .setCellValueFactory(data -> new SimpleStringProperty(formatShares(data.getValue().endShares())));
        incEndSharesColumn.setStyle("-fx-alignment: CENTER-RIGHT;");
        incSharesDiffColumn
                .setCellValueFactory(data -> new SimpleStringProperty(formatSharesDiff(data.getValue().sharesDiff())));
        incSharesDiffColumn.setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: #d32f2f;");
        incChangeRatioColumn.setCellValueFactory(
                data -> new SimpleStringProperty(formatChangeRatio(data.getValue().changeRatio())));
        incChangeRatioColumn.setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: #d32f2f;");
        incWeightDiffColumn
                .setCellValueFactory(data -> new SimpleStringProperty(formatWeightDiff(data.getValue().weightDiff())));
        incWeightDiffColumn.setStyle("-fx-alignment: CENTER-RIGHT;");
    }

    private void setupDecreasedTableColumns() {
        decStockCodeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().stockCode()));
        decStockNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().stockName()));
        decStartSharesColumn
                .setCellValueFactory(data -> new SimpleStringProperty(formatShares(data.getValue().startShares())));
        decStartSharesColumn.setStyle("-fx-alignment: CENTER-RIGHT;");
        decEndSharesColumn
                .setCellValueFactory(data -> new SimpleStringProperty(formatShares(data.getValue().endShares())));
        decEndSharesColumn.setStyle("-fx-alignment: CENTER-RIGHT;");
        decSharesDiffColumn
                .setCellValueFactory(data -> new SimpleStringProperty(formatSharesDiff(data.getValue().sharesDiff())));
        decSharesDiffColumn.setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: #388e3c;");
        decChangeRatioColumn.setCellValueFactory(
                data -> new SimpleStringProperty(formatChangeRatio(data.getValue().changeRatio())));
        decChangeRatioColumn.setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: #388e3c;");
        decWeightDiffColumn
                .setCellValueFactory(data -> new SimpleStringProperty(formatWeightDiff(data.getValue().weightDiff())));
        decWeightDiffColumn.setStyle("-fx-alignment: CENTER-RIGHT;");
    }

    private void setupUnchangedTableColumns() {
        uncStockCodeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().stockCode()));
        uncStockNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().stockName()));
        uncSharesColumn
                .setCellValueFactory(data -> new SimpleStringProperty(formatShares(data.getValue().endShares())));
        uncSharesColumn.setStyle("-fx-alignment: CENTER-RIGHT;");
        uncStartWeightColumn
                .setCellValueFactory(data -> new SimpleStringProperty(formatWeight(data.getValue().startWeight())));
        uncStartWeightColumn.setStyle("-fx-alignment: CENTER-RIGHT;");
        uncEndWeightColumn
                .setCellValueFactory(data -> new SimpleStringProperty(formatWeight(data.getValue().endWeight())));
        uncEndWeightColumn.setStyle("-fx-alignment: CENTER-RIGHT;");
        uncWeightDiffColumn
                .setCellValueFactory(data -> new SimpleStringProperty(formatWeightDiff(data.getValue().weightDiff())));
        uncWeightDiffColumn.setStyle("-fx-alignment: CENTER-RIGHT;");
    }

    /**
     * 載入可用日期
     */
    private void loadAvailableDates() {
        Task<List<LocalDate>> task = new Task<>() {
            @Override
            protected List<LocalDate> call() {
                return storageService.getAvailableDates();
            }
        };

        task.setOnSucceeded(event -> {
            availableDates = task.getValue();
            setupDateCellFactory();

            if (!availableDates.isEmpty()) {
                // 設定預設為最新兩個日期
                if (availableDates.size() >= 2) {
                    endDatePicker.setValue(availableDates.get(0));
                    startDatePicker.setValue(availableDates.get(1));
                } else {
                    endDatePicker.setValue(availableDates.get(0));
                    startDatePicker.setValue(availableDates.get(0));
                }
            }

            logger.info("已載入 {} 個可用日期", availableDates.size());
        });

        task.setOnFailed(event -> {
            logger.error("載入可用日期失敗", task.getException());
        });

        new Thread(task).start();
    }

    // ========== 事件處理器 ==========

    /**
     * 處理返回按鈕點擊
     */
    @FXML
    private void handleBack() {
        if (backHandler != null) {
            backHandler.run();
        }
    }

    /**
     * 處理比較按鈕點擊
     */
    @FXML
    private void handleCompare() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        if (startDate == null || endDate == null) {
            showAlert("請選擇起始與結束日期");
            return;
        }

        if (startDate.isAfter(endDate)) {
            showAlert("起始日期必須早於或等於結束日期");
            return;
        }

        compareButton.setDisable(true);
        setLoading(true, "正在比較...");

        Task<RangeCompareResultDto> task = new Task<>() {
            @Override
            protected RangeCompareResultDto call() {
                return holdingCompareService.compareHoldings(startDate, endDate);
            }
        };

        task.setOnSucceeded(event -> {
            RangeCompareResultDto result = task.getValue();
            displayCompareResult(result);
            compareButton.setDisable(false);
            setLoading(false, "比較完成");
        });

        task.setOnFailed(event -> {
            logger.error("比較失敗", task.getException());
            showAlert("比較失敗: " + task.getException().getMessage());
            compareButton.setDisable(false);
            setLoading(false, "比較失敗");
        });

        new Thread(task).start();
    }

    /**
     * 顯示快速選擇選單
     */
    @FXML
    private void showQuickSelectMenu() {
        ContextMenu menu = new ContextMenu();

        MenuItem week = new MenuItem("近一週");
        week.setOnAction(e -> quickSelect(7));

        MenuItem twoWeeks = new MenuItem("近兩週");
        twoWeeks.setOnAction(e -> quickSelect(14));

        MenuItem month = new MenuItem("近一個月");
        month.setOnAction(e -> quickSelect(30));

        MenuItem threeMonths = new MenuItem("近三個月");
        threeMonths.setOnAction(e -> quickSelect(90));

        menu.getItems().addAll(week, twoWeeks, month, threeMonths);
        menu.show(compareButton, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    private void quickSelect(int days) {
        if (availableDates != null && !availableDates.isEmpty()) {
            LocalDate endDate = availableDates.get(0);
            LocalDate targetStartDate = endDate.minusDays(days);

            // 找到最接近目標起始日期的可用日期
            LocalDate startDate = availableDates.stream()
                    .filter(d -> !d.isAfter(targetStartDate))
                    .findFirst()
                    .orElse(availableDates.get(availableDates.size() - 1));

            endDatePicker.setValue(endDate);
            startDatePicker.setValue(startDate);
        }
    }

    /**
     * 顯示比較結果
     */
    private void displayCompareResult(RangeCompareResultDto result) {
        Platform.runLater(() -> {
            // 更新摘要
            startDateLabel.setText(result.startDate().format(DATE_FORMATTER));
            endDateLabel.setText(result.endDate().format(DATE_FORMATTER));
            newAdditionsLabel.setText(String.valueOf(result.newAdditionsCount()));
            removalsLabel.setText(String.valueOf(result.removalsCount()));
            increasedLabel.setText(String.valueOf(result.increasedCount()));
            decreasedLabel.setText(String.valueOf(result.decreasedCount()));
            unchangedLabel.setText(String.valueOf(result.unchangedCount()));

            // 保存計數
            newAdditionsCount = result.newAdditionsCount();
            removalsCount = result.removalsCount();
            increasedCount = result.increasedCount();
            decreasedCount = result.decreasedCount();
            unchangedCount = result.unchangedCount();

            // 更新表格資料
            newAdditionsData.setAll(result.newAdditions());
            removalsData.setAll(result.removals());
            increasedData.setAll(result.increased());
            decreasedData.setAll(result.decreased());
            unchangedData.setAll(result.unchanged());

            // 自動切換到有資料的標籤頁
            selectFirstNonEmptyTab(result);
        });
    }

    private void selectFirstNonEmptyTab(RangeCompareResultDto result) {
        if (result.newAdditionsCount() > 0) {
            categoryTabPane.getSelectionModel().select(0);
            updateRecordCount(0);
        } else if (result.removalsCount() > 0) {
            categoryTabPane.getSelectionModel().select(1);
            updateRecordCount(1);
        } else if (result.increasedCount() > 0) {
            categoryTabPane.getSelectionModel().select(2);
            updateRecordCount(2);
        } else if (result.decreasedCount() > 0) {
            categoryTabPane.getSelectionModel().select(3);
            updateRecordCount(3);
        } else {
            categoryTabPane.getSelectionModel().select(4);
            updateRecordCount(4);
        }
    }

    // ========== 格式化方法 ==========

    private String formatShares(Long shares) {
        if (shares == null) {
            return "-";
        }
        return NUMBER_FORMAT.format(shares);
    }

    private String formatSharesDiff(Long diff) {
        if (diff == null) {
            return "-";
        }
        String formatted = NUMBER_FORMAT.format(Math.abs(diff));
        return diff >= 0 ? "+" + formatted : "-" + formatted;
    }

    private String formatWeight(BigDecimal weight) {
        if (weight == null) {
            return "-";
        }
        return String.format("%.2f%%", weight);
    }

    private String formatWeightDiff(BigDecimal diff) {
        if (diff == null) {
            return "-";
        }
        String sign = diff.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        return sign + String.format("%.4f", diff);
    }

    private String formatChangeRatio(BigDecimal ratio) {
        if (ratio == null) {
            return "-";
        }
        String sign = ratio.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        return sign + String.format("%.2f%%", ratio);
    }

    private void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("提示");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * 設定加載狀態
     */
    private void setLoading(boolean loading, String message) {
        Platform.runLater(() -> {
            compareButton.setDisable(loading);
            loadingOverlay.setVisible(loading);
            progressLabel.setText(message);
        });
    }
}
