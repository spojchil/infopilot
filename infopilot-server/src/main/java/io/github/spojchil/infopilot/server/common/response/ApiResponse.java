package io.github.spojchil.infopilot.server.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

/** 统一 API 响应体。成功时 {@code data} 为业务数据，失败时 {@code error} 为错误详情。 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ApiError error;

    private ApiResponse(boolean success, T data, ApiError error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, null, null);
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode) {
        return new ApiResponse<>(
                false, null, new ApiError(errorCode.getCode(), errorCode.getMessage()));
    }

    public static <T> ApiResponse<T> failure(int code, String message) {
        return new ApiResponse<>(false, null, new ApiError(code, message));
    }

    @Getter
    public static class ApiError {
        private final int code;
        private final String message;

        public ApiError(int code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
