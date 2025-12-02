package com.etf.tracker.exception;

import java.nio.file.Path;

/**
 * 儲存例外
 * <p>
 * 當 Excel 檔案讀寫失敗時拋出
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
public class StorageException extends RuntimeException {

    /** 錯誤碼 */
    private final String errorCode;

    /** 相關檔案路徑 */
    private final String filePath;

    /** 操作類型 */
    private final Operation operation;

    /**
     * 儲存操作類型
     */
    public enum Operation {
        READ("讀取"),
        WRITE("寫入"),
        CREATE("建立"),
        DELETE("刪除"),
        BACKUP("備份");

        private final String displayName;

        Operation(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 建立儲存例外
     *
     * @param message 錯誤訊息
     */
    public StorageException(String message) {
        super(message);
        this.errorCode = "STORAGE_ERROR";
        this.filePath = null;
        this.operation = null;
    }

    /**
     * 建立儲存例外（含原因）
     *
     * @param message 錯誤訊息
     * @param cause   原始例外
     */
    public StorageException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "STORAGE_ERROR";
        this.filePath = null;
        this.operation = null;
    }

    /**
     * 建立儲存例外（含完整資訊）
     *
     * @param message   錯誤訊息
     * @param errorCode 錯誤碼
     * @param filePath  檔案路徑
     * @param operation 操作類型
     */
    public StorageException(String message, String errorCode, String filePath, Operation operation) {
        super(message);
        this.errorCode = errorCode;
        this.filePath = filePath;
        this.operation = operation;
    }

    /**
     * 建立儲存例外（含完整資訊與原因）
     *
     * @param message   錯誤訊息
     * @param cause     原始例外
     * @param errorCode 錯誤碼
     * @param filePath  檔案路徑
     * @param operation 操作類型
     */
    public StorageException(String message, Throwable cause, String errorCode,
            String filePath, Operation operation) {
        super(message, cause);
        this.errorCode = errorCode;
        this.filePath = filePath;
        this.operation = operation;
    }

    /**
     * 建立讀取錯誤例外
     *
     * @param path  檔案路徑
     * @param cause 原始例外
     * @return StorageException
     */
    public static StorageException readError(Path path, Throwable cause) {
        return new StorageException(
                "讀取檔案失敗: " + path,
                cause,
                "FILE_READ_ERROR",
                path.toString(),
                Operation.READ);
    }

    /**
     * 建立寫入錯誤例外
     *
     * @param path  檔案路徑
     * @param cause 原始例外
     * @return StorageException
     */
    public static StorageException writeError(Path path, Throwable cause) {
        return new StorageException(
                "寫入檔案失敗: " + path,
                cause,
                "FILE_WRITE_ERROR",
                path.toString(),
                Operation.WRITE);
    }

    /**
     * 建立檔案不存在例外
     *
     * @param path 檔案路徑
     * @return StorageException
     */
    public static StorageException fileNotFound(Path path) {
        return new StorageException(
                "檔案不存在: " + path,
                "FILE_NOT_FOUND",
                path.toString(),
                Operation.READ);
    }

    /**
     * 建立格式錯誤例外
     *
     * @param path  檔案路徑
     * @param cause 原始例外
     * @return StorageException
     */
    public static StorageException formatError(Path path, Throwable cause) {
        return new StorageException(
                "檔案格式錯誤: " + path,
                cause,
                "FILE_FORMAT_ERROR",
                path.toString(),
                Operation.READ);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getFilePath() {
        return filePath;
    }

    public Operation getOperation() {
        return operation;
    }
}
