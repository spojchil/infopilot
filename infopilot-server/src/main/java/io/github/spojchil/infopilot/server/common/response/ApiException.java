package io.github.spojchil.infopilot.server.common.response;

import lombok.Getter;

/** 业务异常。Service 层抛出，由 {@code GlobalExceptionHandler} 统一捕获并转为 {@link ApiResponse}。 */
@Getter
public class ApiException extends RuntimeException {

    private final int code;

    public ApiException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public ApiException(int code, String message) {
        super(message);
        this.code = code;
    }

    public ApiException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
    }

    public ApiException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
