package io.github.spojchil.infopilot.server.common.handler;

import static org.junit.jupiter.api.Assertions.*;

import io.github.spojchil.infopilot.server.common.response.ApiException;
import io.github.spojchil.infopilot.server.common.response.ApiResponse;
import io.github.spojchil.infopilot.server.common.response.CommonErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("GlobalExceptionHandler 全局异常处理单元测试")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("ApiException → 400 + PARAM_INVALID")
    void apiExceptionReturnsBadRequest() {
        ApiException ex = new ApiException(CommonErrorCode.PARAM_INVALID);

        ResponseEntity<ApiResponse<Void>> resp = handler.handleApiException(ex);

        assertEquals(400, resp.getStatusCode().value());
        assertFalse(resp.getBody().isSuccess());
        assertEquals(1001, resp.getBody().getError().getCode());
    }

    @Test
    @DisplayName("ApiException 认证错误 → 401")
    void apiExceptionReturnsUnauthorized() {
        ApiException ex = new ApiException(CommonErrorCode.UNAUTHORIZED);

        assertEquals(401, handler.handleApiException(ex).getStatusCode().value());
    }

    @Test
    @DisplayName("ApiException 外部依赖 → 502")
    void apiExceptionReturnsBadGateway() {
        ApiException ex = new ApiException(CommonErrorCode.LLM_CALL_FAILED);

        assertEquals(502, handler.handleApiException(ex).getStatusCode().value());
    }

    @Test
    @DisplayName("未知 Exception → 500 + INTERNAL_ERROR")
    void unknownExceptionReturns500() {
        ResponseEntity<ApiResponse<Void>> resp =
                handler.handleUnknown(new RuntimeException("boom"));

        assertEquals(500, resp.getStatusCode().value());
        assertFalse(resp.getBody().isSuccess());
        assertEquals(5001, resp.getBody().getError().getCode());
    }

    @Test
    @DisplayName("toHttpStatus — 各段号映射正确")
    void errorCodePrefixMapping() {
        assertEquals(HttpStatus.BAD_REQUEST, invokeToHttpStatus(1001));
        assertEquals(HttpStatus.UNAUTHORIZED, invokeToHttpStatus(2001));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, invokeToHttpStatus(5001));
        assertEquals(HttpStatus.BAD_GATEWAY, invokeToHttpStatus(8001));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, invokeToHttpStatus(9999));
    }

    /** 反射调用 private 方法验证映射逻辑。 */
    private static HttpStatus invokeToHttpStatus(int code) {
        try {
            var method = GlobalExceptionHandler.class.getDeclaredMethod("toHttpStatus", int.class);
            method.setAccessible(true);
            return (HttpStatus) method.invoke(null, code);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
