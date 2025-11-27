package com.etf.tracker.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.etf.tracker.dto.ApiResponse;

/**
 * 全域例外處理器
 * <p>
 * 統一處理 REST API 的例外回應
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 處理資料抓取例外
     *
     * @param ex DataFetchException
     * @return 錯誤回應
     */
    @ExceptionHandler(DataFetchException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataFetchException(DataFetchException ex) {
        logger.error("資料抓取失敗: {} [errorCode={}, url={}]",
                ex.getMessage(), ex.getErrorCode(), ex.getTargetUrl(), ex);

        ApiResponse<Void> response = ApiResponse.error(
                ex.getMessage(),
                ex.getErrorCode());

        HttpStatus status = determineHttpStatus(ex);
        return ResponseEntity.status(status).body(response);
    }

    /**
     * 處理儲存例外
     *
     * @param ex StorageException
     * @return 錯誤回應
     */
    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiResponse<Void>> handleStorageException(StorageException ex) {
        logger.error("儲存操作失敗: {} [errorCode={}, filePath={}, operation={}]",
                ex.getMessage(), ex.getErrorCode(), ex.getFilePath(),
                ex.getOperation() != null ? ex.getOperation().getDisplayName() : "未知", ex);

        ApiResponse<Void> response = ApiResponse.error(
                ex.getMessage(),
                ex.getErrorCode());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 處理驗證例外
     *
     * @param ex ValidationException
     * @return 錯誤回應
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(ValidationException ex) {
        logger.warn("驗證失敗: {} [errorCode={}, fieldName={}, invalidValue={}]",
                ex.getMessage(), ex.getErrorCode(), ex.getFieldName(), ex.getInvalidValue());

        ApiResponse<Void> response = ApiResponse.error(
                ex.getMessage(),
                ex.getErrorCode());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 處理非法參數例外
     *
     * @param ex IllegalArgumentException
     * @return 錯誤回應
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("非法參數: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                ex.getMessage(),
                "ILLEGAL_ARGUMENT");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 處理請求參數缺失例外
     *
     * @param ex MissingServletRequestParameterException
     * @return 錯誤回應
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex) {
        logger.warn("缺少必要參數: {} (type: {})", ex.getParameterName(), ex.getParameterType());

        ApiResponse<Void> response = ApiResponse.error(
                String.format("缺少必要參數: %s", ex.getParameterName()),
                "MISSING_PARAMETER");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 處理資源未找到例外 (如 favicon.ico)
     *
     * @param ex NoResourceFoundException
     * @return 錯誤回應
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException ex) {
        // 僅記錄 WARN 或 DEBUG，避免 ERROR 級別日誌干擾
        logger.debug("資源未找到: {}", ex.getResourcePath());

        ApiResponse<Void> response = ApiResponse.error(
                "資源未找到: " + ex.getResourcePath(),
                "RESOURCE_NOT_FOUND");

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * 處理所有其他例外
     *
     * @param ex Exception
     * @return 錯誤回應
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        logger.error("未預期的錯誤: {}", ex.getMessage(), ex);

        ApiResponse<Void> response = ApiResponse.error(
                "系統發生未預期的錯誤，請稍後再試",
                "INTERNAL_ERROR");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 根據 DataFetchException 決定 HTTP 狀態碼
     *
     * @param ex DataFetchException
     * @return HTTP 狀態碼
     */
    private HttpStatus determineHttpStatus(DataFetchException ex) {
        if (ex.getHttpStatus() != null) {
            // 將外部服務的 HTTP 錯誤轉換為適當的回應
            int status = ex.getHttpStatus();
            if (status >= 400 && status < 500) {
                return HttpStatus.BAD_GATEWAY; // 外部服務回傳客戶端錯誤
            } else if (status >= 500) {
                return HttpStatus.BAD_GATEWAY; // 外部服務回傳伺服器錯誤
            }
        }

        String errorCode = ex.getErrorCode();
        if ("CONNECTION_TIMEOUT".equals(errorCode)) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }

        return HttpStatus.SERVICE_UNAVAILABLE;
    }
}
