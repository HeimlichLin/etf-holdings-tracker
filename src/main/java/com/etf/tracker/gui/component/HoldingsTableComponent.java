package com.etf.tracker.gui.component;

import com.etf.tracker.dto.HoldingDto;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * 持倉表格元件
 * <p>
 * 可重用的持倉資料表格，支援排序、過濾和格式化
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
public class HoldingsTableComponent extends VBox {

    private static final String STYLE_CLASS = "holdings-table-component";
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.TAIWAN);

    private final TableView<HoldingDto> tableView;
    private final ObservableList<HoldingDto> data = FXCollections.observableArrayList();
    private final FilteredList<HoldingDto> filteredData;
    private final SortedList<HoldingDto> sortedData;

    private TableColumn<HoldingDto, Integer> indexColumn;
    private TableColumn<HoldingDto, String> stockCodeColumn;
    private TableColumn<HoldingDto, String> stockNameColumn;
    private TableColumn<HoldingDto, String> sharesColumn;
    private TableColumn<HoldingDto, String> weightColumn;

    /**
     * 建立持倉表格元件
     */
    public HoldingsTableComponent() {
        getStyleClass().add(STYLE_CLASS);
        setSpacing(5);

        tableView = new TableView<>();
        tableView.getStyleClass().add("holdings-table");
        VBox.setVgrow(tableView, Priority.ALWAYS);

        setupColumns();

        // 設定過濾與排序
        filteredData = new FilteredList<>(data, p -> true);
        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sortedData);

        // 設定空白提示
        tableView.setPlaceholder(createPlaceholder());

        getChildren().add(tableView);
    }

    /**
     * 設定表格欄位
     */
    private void setupColumns() {
        // 序號欄
        indexColumn = new TableColumn<>("#");
        indexColumn.setPrefWidth(50);
        indexColumn.setMinWidth(40);
        indexColumn.setSortable(false);
        indexColumn.setCellValueFactory(data -> {
            int index = tableView.getItems().indexOf(data.getValue()) + 1;
            return new SimpleIntegerProperty(index).asObject();
        });
        indexColumn.setStyle("-fx-alignment: CENTER;");

        // 股票代號欄
        stockCodeColumn = new TableColumn<>("股票代號");
        stockCodeColumn.setPrefWidth(100);
        stockCodeColumn.setMinWidth(80);
        stockCodeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().stockCode()));

        // 股票名稱欄
        stockNameColumn = new TableColumn<>("股票名稱");
        stockNameColumn.setPrefWidth(200);
        stockNameColumn.setMinWidth(150);
        stockNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().stockName()));

        // 持股數欄
        sharesColumn = new TableColumn<>("持股數");
        sharesColumn.setPrefWidth(150);
        sharesColumn.setMinWidth(100);
        sharesColumn.setCellValueFactory(data -> {
            Long shares = data.getValue().shares();
            return new SimpleStringProperty(NUMBER_FORMAT.format(shares));
        });
        sharesColumn.setStyle("-fx-alignment: CENTER-RIGHT;");
        sharesColumn.setComparator((s1, s2) -> {
            Long l1 = parseLong(s1);
            Long l2 = parseLong(s2);
            return l1.compareTo(l2);
        });

        // 權重欄
        weightColumn = new TableColumn<>("權重(%)");
        weightColumn.setPrefWidth(100);
        weightColumn.setMinWidth(80);
        weightColumn.setCellValueFactory(data -> {
            BigDecimal weight = data.getValue().weight();
            return new SimpleStringProperty(String.format("%.2f%%", weight));
        });
        weightColumn.setStyle("-fx-alignment: CENTER-RIGHT;");
        weightColumn.setComparator((s1, s2) -> {
            Double d1 = parseDouble(s1);
            Double d2 = parseDouble(s2);
            return d1.compareTo(d2);
        });

        tableView.getColumns().addAll(
                indexColumn, stockCodeColumn, stockNameColumn, sharesColumn, weightColumn);
    }

    /**
     * 建立空白提示
     */
    private Label createPlaceholder() {
        Label placeholder = new Label("尚無資料");
        placeholder.getStyleClass().add("table-placeholder");
        return placeholder;
    }

    /**
     * 設定資料
     *
     * @param holdings 持倉清單
     */
    public void setData(List<HoldingDto> holdings) {
        data.clear();
        if (holdings != null) {
            data.addAll(holdings);
        }
    }

    /**
     * 清除資料
     */
    public void clearData() {
        data.clear();
    }

    /**
     * 設定過濾條件
     *
     * @param predicate 過濾條件
     */
    public void setFilter(Predicate<HoldingDto> predicate) {
        filteredData.setPredicate(predicate);
    }

    /**
     * 清除過濾條件
     */
    public void clearFilter() {
        filteredData.setPredicate(p -> true);
    }

    /**
     * 依關鍵字過濾
     *
     * @param keyword 關鍵字
     */
    public void filterByKeyword(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            clearFilter();
            return;
        }

        String lowerKeyword = keyword.toLowerCase();
        setFilter(holding -> holding.stockCode().toLowerCase().contains(lowerKeyword) ||
                holding.stockName().toLowerCase().contains(lowerKeyword));
    }

    /**
     * 取得顯示的資料數量
     *
     * @return 資料數量
     */
    public int getDisplayCount() {
        return sortedData.size();
    }

    /**
     * 取得全部資料數量
     *
     * @return 資料數量
     */
    public int getTotalCount() {
        return data.size();
    }

    /**
     * 取得內部的 TableView
     *
     * @return TableView
     */
    public TableView<HoldingDto> getTableView() {
        return tableView;
    }

    /**
     * 設定是否顯示序號欄
     *
     * @param visible 是否顯示
     */
    public void setIndexColumnVisible(boolean visible) {
        indexColumn.setVisible(visible);
    }

    /**
     * 取得選中的項目
     *
     * @return 選中的項目
     */
    public HoldingDto getSelectedItem() {
        return tableView.getSelectionModel().getSelectedItem();
    }

    /**
     * 選擇指定項目
     *
     * @param holding 要選擇的項目
     */
    public void select(HoldingDto holding) {
        tableView.getSelectionModel().select(holding);
    }

    /**
     * 清除選擇
     */
    public void clearSelection() {
        tableView.getSelectionModel().clearSelection();
    }

    /**
     * 捲動到指定項目
     *
     * @param holding 項目
     */
    public void scrollTo(HoldingDto holding) {
        tableView.scrollTo(holding);
    }

    /**
     * 重新整理表格
     */
    public void refresh() {
        tableView.refresh();
    }

    /**
     * 解析長整數
     */
    private Long parseLong(String s) {
        try {
            return NUMBER_FORMAT.parse(s.replaceAll("[^\\d]", "")).longValue();
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * 解析浮點數
     */
    private Double parseDouble(String s) {
        try {
            return Double.parseDouble(s.replaceAll("[^\\d.]", ""));
        } catch (Exception e) {
            return 0.0;
        }
    }
}
