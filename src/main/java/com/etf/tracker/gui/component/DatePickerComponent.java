package com.etf.tracker.gui.component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.StringConverter;

/**
 * 日期選擇器元件
 * <p>
 * 增強版的日期選擇器，支援可用日期標示與快捷選擇
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
public class DatePickerComponent extends VBox {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String STYLE_CLASS = "date-picker-component";

    private final DatePicker datePicker;
    private final Label titleLabel;
    private final HBox quickSelectBox;
    private final Set<LocalDate> availableDates = new HashSet<>();
    private final ObjectProperty<LocalDate> selectedDate = new SimpleObjectProperty<>();

    /**
     * 建立日期選擇器元件
     */
    public DatePickerComponent() {
        this("選擇日期");
    }

    /**
     * 建立帶標題的日期選擇器元件
     *
     * @param title 標題
     */
    public DatePickerComponent(String title) {
        getStyleClass().add(STYLE_CLASS);
        setSpacing(8);
        setAlignment(Pos.TOP_LEFT);

        // 標題
        titleLabel = new Label(title);
        titleLabel.getStyleClass().add("date-picker-title");

        // 日期選擇器
        datePicker = new DatePicker();
        datePicker.setPromptText("選擇日期...");
        datePicker.setPrefWidth(150);
        datePicker.setConverter(createStringConverter());
        datePicker.setDayCellFactory(createDayCellFactory());
        datePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            selectedDate.set(newVal);
        });

        // 快捷選擇按鈕
        quickSelectBox = new HBox(5);
        quickSelectBox.setAlignment(Pos.CENTER_LEFT);

        Button todayBtn = createQuickButton("今天", LocalDate.now());
        Button yesterdayBtn = createQuickButton("昨天", LocalDate.now().minusDays(1));
        Button lastWeekBtn = createQuickButton("上週", LocalDate.now().minusWeeks(1));

        quickSelectBox.getChildren().addAll(todayBtn, yesterdayBtn, lastWeekBtn);

        getChildren().addAll(titleLabel, datePicker, quickSelectBox);
    }

    /**
     * 設定可用日期
     *
     * @param dates 可用日期清單
     */
    public void setAvailableDates(List<LocalDate> dates) {
        availableDates.clear();
        availableDates.addAll(dates);

        // 更新日期選擇器的 cell factory
        datePicker.setDayCellFactory(createDayCellFactory());

        // 如果有可用日期，預設選擇最新的
        if (!dates.isEmpty()) {
            datePicker.setValue(dates.get(0));
        }
    }

    /**
     * 取得選擇的日期
     *
     * @return 選擇的日期
     */
    public LocalDate getSelectedDate() {
        return selectedDate.get();
    }

    /**
     * 設定選擇的日期
     *
     * @param date 日期
     */
    public void setSelectedDate(LocalDate date) {
        datePicker.setValue(date);
    }

    /**
     * 取得選擇日期屬性
     *
     * @return 日期屬性
     */
    public ObjectProperty<LocalDate> selectedDateProperty() {
        return selectedDate;
    }

    /**
     * 設定標題
     *
     * @param title 標題
     */
    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    /**
     * 設定是否顯示快捷選擇
     *
     * @param visible 是否顯示
     */
    public void setQuickSelectVisible(boolean visible) {
        quickSelectBox.setVisible(visible);
        quickSelectBox.setManaged(visible);
    }

    /**
     * 設定是否顯示標題
     *
     * @param visible 是否顯示
     */
    public void setTitleVisible(boolean visible) {
        titleLabel.setVisible(visible);
        titleLabel.setManaged(visible);
    }

    /**
     * 取得內部的 DatePicker
     *
     * @return DatePicker
     */
    public DatePicker getDatePicker() {
        return datePicker;
    }

    /**
     * 建立字串轉換器
     */
    private StringConverter<LocalDate> createStringConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date != null ? date.format(DATE_FORMATTER) : "";
            }

            @Override
            public LocalDate fromString(String string) {
                try {
                    return string != null && !string.isEmpty()
                            ? LocalDate.parse(string, DATE_FORMATTER)
                            : null;
                } catch (Exception e) {
                    return null;
                }
            }
        };
    }

    /**
     * 建立日期格式 cell factory
     */
    private Callback<DatePicker, DateCell> createDayCellFactory() {
        return picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);

                if (empty || date == null) {
                    return;
                }

                // 標示可用日期
                if (!availableDates.isEmpty()) {
                    if (availableDates.contains(date)) {
                        getStyleClass().add("available-date");
                        setDisable(false);
                    } else {
                        getStyleClass().add("unavailable-date");
                        setDisable(true);
                        setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #999;");
                    }
                }

                // 標示今天
                if (date.equals(LocalDate.now())) {
                    getStyleClass().add("today");
                }

                // 標示未來日期（禁用）
                if (date.isAfter(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #ccc;");
                }
            }
        };
    }

    /**
     * 建立快捷選擇按鈕
     */
    private Button createQuickButton(String text, LocalDate date) {
        Button button = new Button(text);
        button.getStyleClass().add("quick-select-button");
        button.setOnAction(e -> {
            if (availableDates.isEmpty() || availableDates.contains(date)) {
                datePicker.setValue(date);
            } else {
                // 找最接近的可用日期
                availableDates.stream()
                        .filter(d -> !d.isAfter(date))
                        .max(LocalDate::compareTo)
                        .ifPresent(datePicker::setValue);
            }
        });
        return button;
    }
}
