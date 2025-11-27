package com.etf.tracker.exception;

/**
 * 資料抓取例外
 * <p>
 * 當從外部網站抓取資料失敗時拋出
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
public class DataFetchException extends RuntimeException {

    /** 錯誤碼 */
    private final String errorCode;

    /** HTTP 狀態碼（如適用） */
    private final Integer httpStatus;

    /** 目標 URL */
    private final String targetUrl;

    /**
     * 建立資料抓取例外
     *
     * @param message 錯誤訊息
     */
    public DataFetchException(String message) {
        super(message);
        this.errorCode = "DATA_FETCH_ERROR";
        this.httpStatus = null;
        this.targetUrl = null;
    }

    /**
     * 建立資料抓取例外（含原因）
     *
     * @param message 錯誤訊息
     * @param cause   原始例外
     */
    public DataFetchException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "DATA_FETCH_ERROR";
        this.httpStatus = null;
        this.targetUrl = null;
    }

    /**
     * 建立資料抓取例外（含完整資訊）
     *
     * @param message    錯誤訊息
     * @param errorCode  錯誤碼
     * @param httpStatus HTTP 狀態碼
     * @param targetUrl  目標 URL
     */
    public DataFetchException(String message, String errorCode, Integer httpStatus, String targetUrl) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.targetUrl = targetUrl;
    }

    /**
     * 建立資料抓取例外（含完整資訊與原因）
     *
     * @param message    錯誤訊息
     * @param cause      原始例外
     * @param errorCode  錯誤碼
     * @param httpStatus HTTP 狀態碼
     * @param targetUrl  目標 URL
     */
    public DataFetchException(String message, Throwable cause, String errorCode,
            Integer httpStatus, String targetUrl) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.targetUrl = targetUrl;
    }

    /**
     * 建立連線逾時例外
     *
     * @param url 目標 URL
     * @return DataFetchException
     */
    public static DataFetchException timeout(String url) {
        return new DataFetchException(
                "連線逾時: " + url,
                "CONNECTION_TIMEOUT",
                null,
                url);
    }

    /**
     * 建立 HTTP 錯誤例外
     *
     * @param url        目標 URL
     * @param httpStatus HTTP 狀態碼
     * @return DataFetchException
     */
    public static DataFetchException httpError(String url, int httpStatus) {
        return new DataFetchException(
                String.format("HTTP 錯誤 %d: %s", httpStatus, url),
                "HTTP_ERROR",
                httpStatus,
                url);
    }

    /**
     * 建立解析錯誤例外
     *
     * @param url   目標 URL
     * @param cause 原始例外
     * @return DataFetchException
     */
    public static DataFetchException parseError(String url, Throwable cause) {
        return new DataFetchException(
                "解析網頁內容失敗: " + url,
                cause,
                "PARSE_ERROR",
                null,
                url);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public String getTargetUrl() {
        return targetUrl;
    }
}
