package com.etf.tracker.gui.view;

import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.etf.tracker.dto.HoldingDto;
import com.etf.tracker.model.DailySnapshot;
import com.etf.tracker.model.Holding;
import com.etf.tracker.service.HoldingQueryService;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

/**
 * 單日查詢視圖控制器
 * <p>
 * 處理單日持倉資料的查詢與顯示
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
@Component
public class SingleDayViewController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(SingleDayViewController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.TAIWAN);

    // 查詢工具列
    @FXML
    private DatePicker datePicker;
    @FXML
    private Button queryButton;
    @FXML
    private TextField searchField;
    @FXML
    private Label statusLabel;

    // 摘要資訊
    @FXML
    private Label dataDateLabel;
    @FXML
    private Label stockCountLabel;
    @FXML
    private Label totalWeightLabel;
    @FXML
    private Label dateRangeLabel;

    // 表格
    @FXML
    private TableView<HoldingDto> holdingsTable;
    @FXML
    private TableColumn<HoldingDto, Integer> indexColumn;
    @FXML
    private TableColumn<HoldingDto, String> stockCodeColumn;
    @FXML
    private TableColumn<HoldingDto, String> stockNameColumn;
    @FXML
    private TableColumn<HoldingDto, String> sharesColumn;
    @FXML
    private TableColumn<HoldingDto, String> weightColumn;

    // 底部
    @FXML
    private Label recordCountLabel;
    @FXML
    private ComboBox<String> sortComboBox;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Label progressLabel;

    // 服務
    private final HoldingQueryService holdingQueryService;

    // 資料
    private final ObservableList<HoldingDto> holdingsData = FXCollections.observableArrayList();
    private FilteredList<HoldingDto> filteredData;
    private Runnable backHandler;

    public SingleDayViewController(HoldingQueryService holdingQueryService) {
        this.holdingQueryService = holdingQueryService;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("初始化單日查詢視圖");

        setupTableColumns();
        setupSortComboBox();
        setupSearchFilter();
        loadAvailableDates();

        logger.info("單日查詢視圖初始化完成");
    }

    /**
     * 設定返回處理器
     */
    public void setBackHandler(Runnable handler) {
        this.backHandler = handler;
    }

    /**
     * 設定表格欄位
     */
    private void setupTableColumns() {
        // 序號欄
        indexColumn.setCellValueFactory(data -> {
            int index = holdingsTable.getItems().indexOf(data.getValue()) + 1;
            return new SimpleIntegerProperty(index).asObject();
        });
        indexColumn.setStyle("-fx-alignment: CENTER;");

        // 股票代號
        stockCodeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().stockCode()));

        // 股票名稱
        stockNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().stockName()));

        // 持股數（格式化）
        sharesColumn.setCellValueFactory(data -> {
            Long shares = data.getValue().shares();
            return new SimpleStringProperty(NUMBER_FORMAT.format(shares));
        });
        sharesColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        // 權重
        weightColumn.setCellValueFactory(data -> {
            BigDecimal weight = data.getValue().weight();
            return new SimpleStringProperty(String.format("%.2f%%", weight));
        });
        weightColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        // 設定過濾資料
        filteredData = new FilteredList<>(holdingsData, p -> true);
        holdingsTable.setItems(filteredData);
    }

    /**
     * 設定排序下拉選單
     */
    private void setupSortComboBox() {
        sortComboBox.setItems(FXCollections.observableArrayList(
                "權重（高到低）",
                "權重（低到高）",
                "代號（A-Z）",
                "代號（Z-A）"));
        sortComboBox.getSelectionModel().selectFirst();
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
        List<LocalDate> dates = holdingQueryService.getAvailableDates();
        if (!dates.isEmpty()) {
            LocalDate latest = dates.get(0);
            LocalDate earliest = dates.get(dates.size() - 1);
            datePicker.setValue(latest);
            dateRangeLabel.setText(String.format("%s ~ %s",
                    earliest.format(DATE_FORMATTER),
                    latest.format(DATE_FORMATTER)));
        } else {
            dateRangeLabel.setText("無歷史資料");
        }
    }

    /**
     * 處理返回按鈕
     */
    @FXML
    private void handleBack() {
        if (backHandler != null) {
            backHandler.run();
        }
    }

    /**
     * 處理日期選擇
     */
    @FXML
    private void handleDateSelected() {
        handleQuery();
    }

    /**
     * 處理查詢按鈕
     */
    @FXML
    private void handleQuery() {
        LocalDate selectedDate = datePicker.getValue();
        if (selectedDate == null) {
            showWarning("請選擇日期", "請先選擇要查詢的日期");
            return;
        }

        logger.info("查詢 {} 的持倉資料", selectedDate);
        setLoading(true, "載入中...");

        Task<Optional<DailySnapshot>> task = new Task<>() {
            @Override
            protected Optional<DailySnapshot> call() {
                return holdingQueryService.getSnapshotByDate(selectedDate);
            }
        };

        task.setOnSucceeded(event -> {
            Optional<DailySnapshot> snapshot = task.getValue();
            if (snapshot.isPresent()) {
                displaySnapshot(snapshot.get());
                setLoading(false, "查詢完成");
            } else {
                clearDisplay();
                setLoading(false, "無資料");
                showWarning("查無資料", String.format("找不到 %s 的持倉資料", selectedDate));
            }
        });

        task.setOnFailed(event -> {
            logger.error("查詢失敗: {}", task.getException().getMessage());
            setLoading(false, "查詢失敗");
            showError("查詢失敗", task.getException().getMessage());
        });

        new Thread(task).start();
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
     * 處理排序變更
     */
    @FXML
    private void handleSortChange() {
        String selected = sortComboBox.getValue();
        if (selected == null)
            return;

        List<Holding> sorted = switch (selected) {
            case "權重（高到低）" -> holdingQueryService.getHoldingsSortedByWeight(false);
            case "權重（低到高）" -> holdingQueryService.getHoldingsSortedByWeight(true);
            case "代號（A-Z）" -> holdingQueryService.getHoldingsSortedByCode(true);
            case "代號（Z-A）" -> holdingQueryService.getHoldingsSortedByCode(false);
            default -> holdingQueryService.getHoldingsSortedByWeight(false);
        };

        updateTableData(sorted);
    }

    /**
     * 顯示快照資料
     */
    private void displaySnapshot(DailySnapshot snapshot) {
        Platform.runLater(() -> {
            // 更新摘要
            dataDateLabel.setText(snapshot.getDate().format(DATE_FORMATTER));
            stockCountLabel.setText(String.valueOf(snapshot.getTotalCount()));
            totalWeightLabel.setText(String.format("%.2f%%", snapshot.getTotalWeight()));

            // 更新表格
            updateTableData(snapshot.getHoldings());
        });
    }

    /**
     * 更新表格資料
     */
    private void updateTableData(List<Holding> holdings) {
        holdingsData.clear();
        holdings.forEach(h -> holdingsData.add(new HoldingDto(
                h.getStockCode(),
                h.getStockName(),
                h.getShares(),
                h.getWeight())));
        updateRecordCount();
    }

    /**
     * 清除顯示
     */
    private void clearDisplay() {
        Platform.runLater(() -> {
            dataDateLabel.setText("-");
            stockCountLabel.setText("-");
            totalWeightLabel.setText("-");
            holdingsData.clear();
            updateRecordCount();
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
            queryButton.setDisable(loading);
            progressIndicator.setVisible(loading);
            progressLabel.setText(message);
            statusLabel.setText(loading ? "處理中..." : "就緒");
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
}
