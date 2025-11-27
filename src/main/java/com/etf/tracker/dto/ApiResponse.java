package com.etf.tracker.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 通用 API 回應格式
 * <p>
 * 提供統一的 REST API 回應結構
 * </p>
 *
 * @param <T> 回應資料類型
 * @author ETF Tracker Team
 * @version 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /** 是否成功 */
    private boolean success;

    /** 回應訊息 */
    private String message;

    /** 回應資料 */
    private T data;

    /** 錯誤碼 (失敗時使用) */
    private String errorCode;

    /** 時間戳記 */
    private LocalDateTime timestamp;

    /**
     * 預設建構子
     */
    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * 成功回應建構子
     */
    private ApiResponse(boolean success, String message, T data, String errorCode) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * 建立成功回應
     *
     * @param data 回應資料
     * @param <T>  資料類型
     * @return 成功的 ApiResponse
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "操作成功", data, null);
    }

    /**
     * 建立成功回應（含自訂訊息）
     *
     * @param message 成功訊息
     * @param data    回應資料
     * @param <T>     資料類型
     * @return 成功的 ApiResponse
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    /**
     * 建立成功回應（無資料）
     *
     * @param message 成功訊息
     * @param <T>     資料類型
     * @return 成功的 ApiResponse
     */
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null, null);
    }

    /**
     * 建立失敗回應
     *
     * @param message   錯誤訊息
     * @param errorCode 錯誤碼
     * @param <T>       資料類型
     * @return 失敗的 ApiResponse
     */
    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return new ApiResponse<>(false, message, null, errorCode);
    }

    /**
     * 建立失敗回應（無錯誤碼）
     *
     * @param message 錯誤訊息
     * @param <T>     資料類型
     * @return 失敗的 ApiResponse
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, "UNKNOWN_ERROR");
    }

    // Getters and Setters

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
