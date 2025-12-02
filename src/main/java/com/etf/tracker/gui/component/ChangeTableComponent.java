package com.etf.tracker.gui.component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import com.etf.tracker.dto.HoldingChangeDto;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * 持倉變化表格元件
 * <p>
 * 可重用的持倉變化資料表格，支援紅增綠減顏色樣式
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
public class ChangeTableComponent extends VBox {

    /** 增加顏色（紅色） */
    private static final String INCREASE_COLOR = "#d32f2f";

    /** 減少顏色（綠色） */
    private static final String DECREASE_COLOR = "#388e3c";

    /** 中性顏色 */
    private static final String NEUTRAL_COLOR = "#757575";

    private static final String STYLE_CLASS = "change-table-component";
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.TAIWAN);

    private final TableView<HoldingChangeDto> tableView;
    private final ObservableList<HoldingChangeDto> data = FXCollections.observableArrayList();

    /**
     * 表格類型列舉
     */
    public enum TableType {
        /** 新進增持 */
        NEW_ADDITIONS,
        /** 剔除減持 */
        REMOVALS,
        /** 增持 */
        INCREASED,
        /** 減持 */
        DECREASED,
        /** 不變 */
        UNCHANGED
    }

    private final TableType tableType;

    /**
     * 建立持倉變化表格元件
     *
     * @param type 表格類型
     */
    public ChangeTableComponent(TableType type) {
        this.tableType = type;
        getStyleClass().add(STYLE_CLASS);
        setSpacing(5);

        tableView = new TableView<>();
        tableView.getStyleClass().add("change-table");
        VBox.setVgrow(tableView, Priority.ALWAYS);

        setupColumns();
        tableView.setItems(data);
        tableView.setPlaceholder(createPlaceholder());

        getChildren().add(tableView);
    }

    /**
     * 設定表格欄位（根據類型）
     */
    private void setupColumns() {
        switch (tableType) {
            case NEW_ADDITIONS -> setupNewAdditionsColumns();
            case REMOVALS -> setupRemovalsColumns();
            case INCREASED -> setupIncreasedColumns();
            case DECREASED -> setupDecreasedColumns();
            case UNCHANGED -> setupUnchangedColumns();
        }
    }

    private void setupNewAdditionsColumns() {
        TableColumn<HoldingChangeDto, String> codeCol = createStockCodeColumn();
        TableColumn<HoldingChangeDto, String> nameCol = createStockNameColumn();
        TableColumn<HoldingChangeDto, String> sharesCol = createSharesColumn("持股數", true);
        TableColumn<HoldingChangeDto, String> weightCol = createWeightColumn("權重(%)", true);

        // 新進全部顯示紅色
        styleColumnAsIncrease(sharesCol);
        styleColumnAsIncrease(weightCol);

        tableView.getColumns().addAll(codeCol, nameCol, sharesCol, weightCol);
    }

    private void setupRemovalsColumns() {
        TableColumn<HoldingChangeDto, String> codeCol = createStockCodeColumn();
        TableColumn<HoldingChangeDto, String> nameCol = createStockNameColumn();
        TableColumn<HoldingChangeDto, String> sharesCol = createSharesColumn("原持股數", false);
        TableColumn<HoldingChangeDto, String> weightCol = createWeightColumn("原權重(%)", false);

        // 剔除全部顯示綠色
        styleColumnAsDecrease(sharesCol);
        styleColumnAsDecrease(weightCol);

        tableView.getColumns().addAll(codeCol, nameCol, sharesCol, weightCol);
    }

    private void setupIncreasedColumns() {
        TableColumn<HoldingChangeDto, String> codeCol = createStockCodeColumn();
        TableColumn<HoldingChangeDto, String> nameCol = createStockNameColumn();
        TableColumn<HoldingChangeDto, String> startSharesCol = createSharesColumn("起始股數", false);
        TableColumn<HoldingChangeDto, String> endSharesCol = createSharesColumn("結束股數", true);
        TableColumn<HoldingChangeDto, String> diffCol = createSharesDiffColumn();
        TableColumn<HoldingChangeDto, String> ratioCol = createChangeRatioColumn();
        TableColumn<HoldingChangeDto, String> weightDiffCol = createWeightDiffColumn();

        // 增持差異顯示紅色
        styleColumnAsIncrease(diffCol);
        styleColumnAsIncrease(ratioCol);

        tableView.getColumns().addAll(codeCol, nameCol, startSharesCol, endSharesCol,
                diffCol, ratioCol, weightDiffCol);
    }

    private void setupDecreasedColumns() {
        TableColumn<HoldingChangeDto, String> codeCol = createStockCodeColumn();
        TableColumn<HoldingChangeDto, String> nameCol = createStockNameColumn();
        TableColumn<HoldingChangeDto, String> startSharesCol = createSharesColumn("起始股數", false);
        TableColumn<HoldingChangeDto, String> endSharesCol = createSharesColumn("結束股數", true);
        TableColumn<HoldingChangeDto, String> diffCol = createSharesDiffColumn();
        TableColumn<HoldingChangeDto, String> ratioCol = createChangeRatioColumn();
        TableColumn<HoldingChangeDto, String> weightDiffCol = createWeightDiffColumn();

        // 減持差異顯示綠色
        styleColumnAsDecrease(diffCol);
        styleColumnAsDecrease(ratioCol);

        tableView.getColumns().addAll(codeCol, nameCol, startSharesCol, endSharesCol,
                diffCol, ratioCol, weightDiffCol);
    }

    private void setupUnchangedColumns() {
        TableColumn<HoldingChangeDto, String> codeCol = createStockCodeColumn();
        TableColumn<HoldingChangeDto, String> nameCol = createStockNameColumn();
        TableColumn<HoldingChangeDto, String> sharesCol = createSharesColumn("持股數", true);
        TableColumn<HoldingChangeDto, String> startWeightCol = createWeightColumn("起始權重(%)", false);
        TableColumn<HoldingChangeDto, String> endWeightCol = createWeightColumn("結束權重(%)", true);
        TableColumn<HoldingChangeDto, String> weightDiffCol = createWeightDiffColumn();

        tableView.getColumns().addAll(codeCol, nameCol, sharesCol, startWeightCol,
                endWeightCol, weightDiffCol);
    }

    // ========== 欄位建立方法 ==========

    private TableColumn<HoldingChangeDto, String> createStockCodeColumn() {
        TableColumn<HoldingChangeDto, String> col = new TableColumn<>("股票代號");
        col.setPrefWidth(100);
        col.setMinWidth(80);
        col.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().stockCode()));
        return col;
    }

    private TableColumn<HoldingChangeDto, String> createStockNameColumn() {
        TableColumn<HoldingChangeDto, String> col = new TableColumn<>("股票名稱");
        col.setPrefWidth(180);
        col.setMinWidth(120);
        col.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().stockName()));
        return col;
    }

    private TableColumn<HoldingChangeDto, String> createSharesColumn(String title, boolean isEnd) {
        TableColumn<HoldingChangeDto, String> col = new TableColumn<>(title);
        col.setPrefWidth(120);
        col.setMinWidth(100);
        col.setCellValueFactory(data -> {
            Long shares = isEnd ? data.getValue().endShares() : data.getValue().startShares();
            return new SimpleStringProperty(formatShares(shares));
        });
        col.setStyle("-fx-alignment: CENTER-RIGHT;");
        return col;
    }

    private TableColumn<HoldingChangeDto, String> createSharesDiffColumn() {
        TableColumn<HoldingChangeDto, String> col = new TableColumn<>("增減股數");
        col.setPrefWidth(110);
        col.setMinWidth(90);
        col.setCellValueFactory(data -> new SimpleStringProperty(
                formatSharesDiff(data.getValue().sharesDiff())));
        col.setStyle("-fx-alignment: CENTER-RIGHT;");
        return col;
    }

    private TableColumn<HoldingChangeDto, String> createChangeRatioColumn() {
        TableColumn<HoldingChangeDto, String> col = new TableColumn<>("變化率(%)");
        col.setPrefWidth(100);
        col.setMinWidth(80);
        col.setCellValueFactory(data -> new SimpleStringProperty(
                formatChangeRatio(data.getValue().changeRatio())));
        col.setStyle("-fx-alignment: CENTER-RIGHT;");
        return col;
    }

    private TableColumn<HoldingChangeDto, String> createWeightColumn(String title, boolean isEnd) {
        TableColumn<HoldingChangeDto, String> col = new TableColumn<>(title);
        col.setPrefWidth(100);
        col.setMinWidth(80);
        col.setCellValueFactory(data -> {
            BigDecimal weight = isEnd ? data.getValue().endWeight() : data.getValue().startWeight();
            return new SimpleStringProperty(formatWeight(weight));
        });
        col.setStyle("-fx-alignment: CENTER-RIGHT;");
        return col;
    }

    private TableColumn<HoldingChangeDto, String> createWeightDiffColumn() {
        TableColumn<HoldingChangeDto, String> col = new TableColumn<>("權重變化");
        col.setPrefWidth(100);
        col.setMinWidth(80);
        col.setCellValueFactory(data -> new SimpleStringProperty(
                formatWeightDiff(data.getValue().weightDiff())));

        // 根據值動態著色
        col.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER-RIGHT;");

                    // 根據正負值設定顏色
                    if (item.startsWith("+")) {
                        setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: " + INCREASE_COLOR + ";");
                    } else if (item.startsWith("-")) {
                        setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: " + DECREASE_COLOR + ";");
                    }
                }
            }
        });
        return col;
    }

    // ========== 樣式方法 ==========

    private void styleColumnAsIncrease(TableColumn<HoldingChangeDto, String> col) {
        col.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: " + INCREASE_COLOR + ";");
                }
            }
        });
    }

    private void styleColumnAsDecrease(TableColumn<HoldingChangeDto, String> col) {
        col.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: " + DECREASE_COLOR + ";");
                }
            }
        });
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

    private Label createPlaceholder() {
        Label placeholder = new Label(getPlaceholderText());
        placeholder.getStyleClass().add("placeholder-text");
        return placeholder;
    }

    private String getPlaceholderText() {
        return switch (tableType) {
            case NEW_ADDITIONS -> "無新進增持";
            case REMOVALS -> "無剔除減持";
            case INCREASED -> "無增持";
            case DECREASED -> "無減持";
            case UNCHANGED -> "無不變";
        };
    }

    // ========== 公開方法 ==========

    /**
     * 設定表格資料
     *
     * @param changes 變化資料清單
     */
    public void setData(List<HoldingChangeDto> changes) {
        data.setAll(changes);
    }

    /**
     * 清空表格資料
     */
    public void clearData() {
        data.clear();
    }

    /**
     * 取得資料筆數
     *
     * @return 資料筆數
     */
    public int getCount() {
        return data.size();
    }

    /**
     * 取得底層 TableView
     *
     * @return TableView 實例
     */
    public TableView<HoldingChangeDto> getTableView() {
        return tableView;
    }
}
