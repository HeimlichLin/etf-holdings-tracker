package com.etf.tracker.gui.component;

import java.util.function.Consumer;

import com.etf.tracker.gui.component.ConfirmDialog.CleanupConfirmResult;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * 確認對話框元件
 * <p>
 * 提供可自訂的確認對話框，支援：
 * <ul>
 * <li>標題與訊息自訂</li>
 * <li>可選的輸入元件（如天數選擇器）</li>
 * <li>確認與取消回呼</li>
 * <li>危險操作警示樣式</li>
 * </ul>
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
public class ConfirmDialog {

    private final Stage dialogStage;
    private final VBox contentBox;
    private final Label titleLabel;
    private final Label messageLabel;
    private final Label detailLabel;
    private final HBox buttonBox;
    private final Button confirmButton;
    private final Button cancelButton;

    private boolean confirmed = false;
    private Consumer<Boolean> resultCallback;

    /**
     * 建構確認對話框
     *
     * @param owner 父視窗
     */
    public ConfirmDialog(Window owner) {
        dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(owner);
        dialogStage.initStyle(StageStyle.DECORATED);
        dialogStage.setResizable(false);

        // 標題
        titleLabel = new Label();
        titleLabel.getStyleClass().add("dialog-title");
        titleLabel.setWrapText(true);

        // 訊息
        messageLabel = new Label();
        messageLabel.getStyleClass().add("dialog-message");
        messageLabel.setWrapText(true);

        // 詳細資訊
        detailLabel = new Label();
        detailLabel.getStyleClass().add("dialog-detail");
        detailLabel.setWrapText(true);
        detailLabel.setVisible(false);
        detailLabel.setManaged(false);

        // 按鈕
        confirmButton = new Button("確認");
        confirmButton.getStyleClass().addAll("dialog-button", "confirm-button");
        confirmButton.setDefaultButton(true);
        confirmButton.setOnAction(e -> {
            confirmed = true;
            dialogStage.close();
            if (resultCallback != null) {
                resultCallback.accept(true);
            }
        });

        cancelButton = new Button("取消");
        cancelButton.getStyleClass().addAll("dialog-button", "cancel-button");
        cancelButton.setCancelButton(true);
        cancelButton.setOnAction(e -> {
            confirmed = false;
            dialogStage.close();
            if (resultCallback != null) {
                resultCallback.accept(false);
            }
        });

        buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.getChildren().addAll(cancelButton, confirmButton);

        // 內容容器
        contentBox = new VBox(16);
        contentBox.setPadding(new Insets(24));
        contentBox.setAlignment(Pos.TOP_LEFT);
        contentBox.getChildren().addAll(titleLabel, messageLabel, detailLabel, buttonBox);
        VBox.setVgrow(messageLabel, Priority.ALWAYS);

        Scene scene = new Scene(contentBox, 400, 200);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        dialogStage.setScene(scene);
    }

    /**
     * 設定對話框標題
     *
     * @param title 標題文字
     * @return this
     */
    public ConfirmDialog title(String title) {
        dialogStage.setTitle(title);
        titleLabel.setText(title);
        return this;
    }

    /**
     * 設定對話框訊息
     *
     * @param message 訊息文字
     * @return this
     */
    public ConfirmDialog message(String message) {
        messageLabel.setText(message);
        return this;
    }

    /**
     * 設定詳細資訊
     *
     * @param detail 詳細資訊文字
     * @return this
     */
    public ConfirmDialog detail(String detail) {
        if (detail != null && !detail.isEmpty()) {
            detailLabel.setText(detail);
            detailLabel.setVisible(true);
            detailLabel.setManaged(true);
        }
        return this;
    }

    /**
     * 設定確認按鈕文字
     *
     * @param text 按鈕文字
     * @return this
     */
    public ConfirmDialog confirmText(String text) {
        confirmButton.setText(text);
        return this;
    }

    /**
     * 設定取消按鈕文字
     *
     * @param text 按鈕文字
     * @return this
     */
    public ConfirmDialog cancelText(String text) {
        cancelButton.setText(text);
        return this;
    }

    /**
     * 設定為危險操作樣式
     *
     * @return this
     */
    public ConfirmDialog danger() {
        confirmButton.getStyleClass().add("danger-button");
        titleLabel.getStyleClass().add("danger-title");
        return this;
    }

    /**
     * 設定為警告操作樣式
     *
     * @return this
     */
    public ConfirmDialog warning() {
        confirmButton.getStyleClass().add("warning-button");
        titleLabel.getStyleClass().add("warning-title");
        return this;
    }

    /**
     * 設定結果回呼
     *
     * @param callback 回呼函數
     * @return this
     */
    public ConfirmDialog onResult(Consumer<Boolean> callback) {
        this.resultCallback = callback;
        return this;
    }

    /**
     * 顯示對話框並等待結果
     *
     * @return true 如果使用者確認
     */
    public boolean showAndWait() {
        dialogStage.showAndWait();
        return confirmed;
    }

    /**
     * 顯示對話框（非阻塞）
     */
    public void show() {
        dialogStage.show();
    }

    /**
     * 關閉對話框
     */
    public void close() {
        dialogStage.close();
    }

    // ========== 靜態工廠方法 ==========

    /**
     * 建立簡單確認對話框
     *
     * @param owner   父視窗
     * @param title   標題
     * @param message 訊息
     * @return 確認對話框
     */
    public static ConfirmDialog confirm(Window owner, String title, String message) {
        return new ConfirmDialog(owner)
                .title(title)
                .message(message);
    }

    /**
     * 建立危險操作確認對話框
     *
     * @param owner   父視窗
     * @param title   標題
     * @param message 訊息
     * @return 確認對話框
     */
    public static ConfirmDialog dangerConfirm(Window owner, String title, String message) {
        return new ConfirmDialog(owner)
                .title(title)
                .message(message)
                .danger()
                .confirmText("確認刪除");
    }

    /**
     * 建立資料清理確認對話框
     *
     * @param owner        父視窗
     * @param daysToKeep   保留天數
     * @param expiredCount 將被刪除的記錄數
     * @return 確認對話框
     */
    public static ConfirmDialog cleanupConfirm(Window owner, int daysToKeep, int expiredCount) {
        String message = String.format(
                "這將刪除 %d 天前的所有資料。\n\n預計刪除 %d 筆記錄。\n\n此操作無法復原，確定要繼續嗎？",
                daysToKeep, expiredCount);

        return new ConfirmDialog(owner)
                .title("確認清理資料")
                .message(message)
                .danger()
                .confirmText("確認清理");
    }

    /**
     * 清理確認對話框（含天數選擇器）
     * <p>
     * 建立包含天數選擇 Spinner 的清理確認對話框
     * 根據選擇的天數動態計算實際會被保留的記錄數（Excel 中的成分股筆數）
     * </p>
     */
    public static class CleanupConfirmDialog extends Dialog<CleanupConfirmResult> {

        private final Spinner<Integer> daysSpinner;
        private final Label infoLabel;
        private final com.etf.tracker.service.ExcelStorageService excelStorageService;

        public CleanupConfirmDialog(int defaultDays, com.etf.tracker.service.ExcelStorageService excelStorageService) {
            setTitle("清理過期資料");
            setHeaderText("設定資料保留天數");

            this.excelStorageService = excelStorageService;

            // 天數選擇器
            daysSpinner = new Spinner<>();
            SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 365,
                    defaultDays);
            daysSpinner.setValueFactory(valueFactory);
            daysSpinner.setEditable(true);
            daysSpinner.setPrefWidth(100);

            Label daysLabel = new Label("保留天數：");
            infoLabel = new Label();
            infoLabel.getStyleClass().add("dialog-info");
            infoLabel.setWrapText(true);

            // 初始化信息
            updateInfo();

            // 當天數改變時更新信息
            daysSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
                updateInfo();
            });

            // 當用戶手動編輯 Spinner 文本框時，失焦後立即更新
            daysSpinner.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (wasFocused && !isFocused) {
                    // 提交編輯的值
                    try {
                        daysSpinner.commitValue();
                    } catch (Exception e) {
                        // 忽略解析錯誤，使用舊值
                    }
                    updateInfo();
                }
            });

            HBox daysBox = new HBox(8, daysLabel, daysSpinner, new Label("天"));
            daysBox.setAlignment(Pos.CENTER_LEFT);

            VBox content = new VBox(16, daysBox, infoLabel);
            content.setPadding(new Insets(16));

            getDialogPane().setContent(content);
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            // 設定按鈕文字
            Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
            okButton.setText("確認清理");
            okButton.getStyleClass().add("danger-button");

            setResultConverter(buttonType -> {
                if (buttonType == ButtonType.OK) {
                    return new CleanupConfirmResult(true, daysSpinner.getValue());
                }
                return new CleanupConfirmResult(false, 0);
            });
        }

        /**
         * 根據選擇的天數更新信息顯示
         * 計算實際會被刪除和保留的記錄數（Excel 中的成分股筆數）
         */
        private void updateInfo() {
            int selectedDays = daysSpinner.getValue();
            // 保留 N 天 = 刪除 N 天前及更早的資料
            // 例如：保留 1 天 = 只保留今天，刪除昨天之前的所有資料
            java.time.LocalDate cutoffDate = java.time.LocalDate.now().minusDays(selectedDays);

            // 從 Excel 中計算符合條件的記錄數（成分股筆數）
            int totalRecords = excelStorageService.getTotalRecordCount();
            int expiredRecords = excelStorageService.countRecordsBefore(cutoffDate);
            int remainingRecords = totalRecords - expiredRecords;

            infoLabel.setText(String.format(
                    "保留天數：%d  |  總資料：%d 筆  |  將被刪除：%d 筆  |  保留：%d 筆",
                    selectedDays, totalRecords, expiredRecords, remainingRecords));
        }

        public int getSelectedDays() {
            return daysSpinner.getValue();
        }
    }

    /**
     * 清理確認結果
     *
     * @param confirmed  是否確認
     * @param daysToKeep 保留天數
     */
    public record CleanupConfirmResult(boolean confirmed, int daysToKeep) {
    }
}
