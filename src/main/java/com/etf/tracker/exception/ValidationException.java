package com.etf.tracker.exception;

import java.util.Collections;
import java.util.List;

/**
 * 驗證例外
 * <p>
 * 當資料驗證失敗時拋出
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
public class ValidationException extends RuntimeException {

    /** 錯誤碼 */
    private final String errorCode;

    /** 驗證失敗的欄位名稱 */
    private final String fieldName;

    /** 驗證失敗的欄位值 */
    private final Object invalidValue;

    /** 多個驗證錯誤 */
    private final List<ValidationError> errors;

    /**
     * 驗證錯誤詳細資訊
     */
    public record ValidationError(
            String fieldName,
            String message,
            Object invalidValue) {
    }

    /**
     * 建立驗證例外
     *
     * @param message 錯誤訊息
     */
    public ValidationException(String message) {
        super(message);
        this.errorCode = "VALIDATION_ERROR";
        this.fieldName = null;
        this.invalidValue = null;
        this.errors = Collections.emptyList();
    }

    /**
     * 建立驗證例外（含欄位資訊）
     *
     * @param message   錯誤訊息
     * @param fieldName 欄位名稱
     */
    public ValidationException(String message, String fieldName) {
        super(message);
        this.errorCode = "VALIDATION_ERROR";
        this.fieldName = fieldName;
        this.invalidValue = null;
        this.errors = List.of(new ValidationError(fieldName, message, null));
    }

    /**
     * 建立驗證例外（含完整資訊）
     *
     * @param message      錯誤訊息
     * @param fieldName    欄位名稱
     * @param invalidValue 無效的值
     */
    public ValidationException(String message, String fieldName, Object invalidValue) {
        super(message);
        this.errorCode = "VALIDATION_ERROR";
        this.fieldName = fieldName;
        this.invalidValue = invalidValue;
        this.errors = List.of(new ValidationError(fieldName, message, invalidValue));
    }

    /**
     * 建立驗證例外（多個錯誤）
     *
     * @param message 錯誤訊息
     * @param errors  驗證錯誤清單
     */
    public ValidationException(String message, List<ValidationError> errors) {
        super(message);
        this.errorCode = "VALIDATION_ERROR";
        this.fieldName = null;
        this.invalidValue = null;
        this.errors = errors != null ? errors : Collections.emptyList();
    }

    /**
     * 建立必填欄位錯誤
     *
     * @param fieldName 欄位名稱
     * @return ValidationException
     */
    public static ValidationException required(String fieldName) {
        return new ValidationException(
                fieldName + " 為必填欄位",
                fieldName);
    }

    /**
     * 建立無效值錯誤
     *
     * @param fieldName    欄位名稱
     * @param invalidValue 無效的值
     * @return ValidationException
     */
    public static ValidationException invalid(String fieldName, Object invalidValue) {
        return new ValidationException(
                fieldName + " 的值無效: " + invalidValue,
                fieldName,
                invalidValue);
    }

    /**
     * 建立日期範圍錯誤
     *
     * @param startDate 起始日期
     * @param endDate   結束日期
     * @return ValidationException
     */
    public static ValidationException invalidDateRange(Object startDate, Object endDate) {
        return new ValidationException(
                String.format("日期範圍無效: 起始日期 %s 不可晚於結束日期 %s", startDate, endDate),
                "dateRange");
    }

    /**
     * 建立超出範圍錯誤
     *
     * @param fieldName 欄位名稱
     * @param value     值
     * @param min       最小值
     * @param max       最大值
     * @return ValidationException
     */
    public static ValidationException outOfRange(String fieldName, Object value, Object min, Object max) {
        return new ValidationException(
                String.format("%s 的值 %s 超出範圍 [%s, %s]", fieldName, value, min, max),
                fieldName,
                value);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getInvalidValue() {
        return invalidValue;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public boolean hasMultipleErrors() {
        return errors.size() > 1;
    }
}
