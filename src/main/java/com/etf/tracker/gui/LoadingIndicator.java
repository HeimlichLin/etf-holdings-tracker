package com.etf.tracker.gui;

import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

/**
 * 載入指示器元件
 * <p>
 * 自訂的圓形載入動畫元件
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
public class LoadingIndicator extends VBox {

    private static final double DEFAULT_SIZE = 50;
    private static final Duration DEFAULT_DURATION = Duration.seconds(1);
    private static final Color DEFAULT_COLOR = Color.web("#2196F3");

    private final Arc spinner;
    private final Label messageLabel;
    private final RotateTransition rotateTransition;
    private final FadeTransition fadeTransition;

    /**
     * 建立預設大小的載入指示器
     */
    public LoadingIndicator() {
        this(DEFAULT_SIZE);
    }

    /**
     * 建立指定大小的載入指示器
     *
     * @param size 指示器大小
     */
    public LoadingIndicator(double size) {
        this(size, DEFAULT_COLOR);
    }

    /**
     * 建立指定大小和顏色的載入指示器
     *
     * @param size  指示器大小
     * @param color 指示器顏色
     */
    public LoadingIndicator(double size, Color color) {
        setAlignment(Pos.CENTER);
        setSpacing(10);

        // 建立旋轉弧形
        spinner = new Arc();
        spinner.setCenterX(size / 2);
        spinner.setCenterY(size / 2);
        spinner.setRadiusX(size / 2 - 5);
        spinner.setRadiusY(size / 2 - 5);
        spinner.setStartAngle(0);
        spinner.setLength(270);
        spinner.setType(ArcType.OPEN);
        spinner.setFill(Color.TRANSPARENT);
        spinner.setStroke(color);
        spinner.setStrokeWidth(4);
        spinner.setStrokeLineCap(StrokeLineCap.ROUND);

        // 建立背景圓形
        Circle background = new Circle();
        background.setCenterX(size / 2);
        background.setCenterY(size / 2);
        background.setRadius(size / 2 - 5);
        background.setFill(Color.TRANSPARENT);
        background.setStroke(color.deriveColor(0, 1, 1, 0.2));
        background.setStrokeWidth(4);

        // 容器
        javafx.scene.layout.StackPane spinnerContainer = new javafx.scene.layout.StackPane();
        spinnerContainer.getChildren().addAll(background, spinner);
        spinnerContainer.setPrefSize(size, size);
        spinnerContainer.setMaxSize(size, size);

        // 訊息標籤
        messageLabel = new Label();
        messageLabel.getStyleClass().add("loading-message");

        getChildren().addAll(spinnerContainer, messageLabel);

        // 旋轉動畫
        rotateTransition = new RotateTransition(DEFAULT_DURATION, spinner);
        rotateTransition.setByAngle(360);
        rotateTransition.setCycleCount(RotateTransition.INDEFINITE);
        rotateTransition.setInterpolator(javafx.animation.Interpolator.LINEAR);

        // 淡入淡出動畫
        fadeTransition = new FadeTransition(Duration.millis(300), this);

        setVisible(false);
    }

    /**
     * 開始載入動畫
     */
    public void start() {
        start(null);
    }

    /**
     * 開始載入動畫並顯示訊息
     *
     * @param message 訊息
     */
    public void start(String message) {
        setMessage(message);
        setVisible(true);

        fadeTransition.setFromValue(0);
        fadeTransition.setToValue(1);
        fadeTransition.play();

        rotateTransition.play();
    }

    /**
     * 停止載入動畫
     */
    public void stop() {
        rotateTransition.stop();

        fadeTransition.setFromValue(1);
        fadeTransition.setToValue(0);
        fadeTransition.setOnFinished(e -> setVisible(false));
        fadeTransition.play();
    }

    /**
     * 設定訊息
     *
     * @param message 訊息
     */
    public void setMessage(String message) {
        messageLabel.setText(message != null ? message : "");
        messageLabel.setVisible(message != null && !message.isEmpty());
    }

    /**
     * 設定顏色
     *
     * @param color 顏色
     */
    public void setColor(Color color) {
        spinner.setStroke(color);
    }

    /**
     * 設定旋轉速度
     *
     * @param duration 一圈的時間
     */
    public void setSpeed(Duration duration) {
        rotateTransition.setDuration(duration);
    }

    /**
     * 檢查是否正在載入
     *
     * @return 是否正在載入
     */
    public boolean isLoading() {
        return isVisible() && rotateTransition.getStatus() == javafx.animation.Animation.Status.RUNNING;
    }
}
