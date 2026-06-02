package io.github.spojchil.infopilot.server.common.handler;

import io.github.spojchil.infopilot.server.common.response.ApiException;
import io.github.spojchil.infopilot.server.common.response.ApiResponse;
import io.github.spojchil.infopilot.server.common.response.CommonErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理。业务异常返回对应的 HTTP 状态码和错误信息，未知异常记录完整栈并返回 500。
 *
 * <p>注意：Spring Boot 4 / Spring 7 移除了 {@code NoResourceFoundException}，404 由框架自行处理。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：按错误码前缀映射 HTTP 状态。 */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        log.warn("业务异常: code={}, message={}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(toHttpStatus(ex.getCode()))
                .body(ApiResponse.failure(ex.getCode(), ex.getMessage()));
    }

    /** 参数校验异常。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String msg =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(e -> e.getField() + ": " + e.getDefaultMessage())
                        .reduce((a, b) -> a + "; " + b)
                        .orElse("参数校验失败");
        log.warn("参数校验失败: {}", msg);
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(CommonErrorCode.PARAM_INVALID.getCode(), msg));
    }

    /** 未知异常：记录完整栈，返回通用错误。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex) {
        log.error("未捕获异常", ex);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.failure(CommonErrorCode.INTERNAL_ERROR));
    }

    /**
     * 错误码前缀 → HTTP 状态。
     *
     * <pre>
     * 1xxx → 400 (参数/资源)
     * 2xxx → 401 (认证/授权)
     * 5xxx → 500 (服务端)
     * 8xxx → 502 (外部依赖)
     * </pre>
     */
    private static HttpStatus toHttpStatus(int code) {
        return switch (code / 1000) {
            case 1 -> HttpStatus.BAD_REQUEST;
            case 2 -> HttpStatus.UNAUTHORIZED;
            case 5 -> HttpStatus.INTERNAL_SERVER_ERROR;
            case 8 -> HttpStatus.BAD_GATEWAY;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
