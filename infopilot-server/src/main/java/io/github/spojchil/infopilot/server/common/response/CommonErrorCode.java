package io.github.spojchil.infopilot.server.common.response;

import lombok.Getter;

/**
 * 通用错误码。编号规范：
 *
 * <pre>
 * 0xxx   成功
 * 1xxx   参数 / 资源
 * 2xxx   认证 / 授权
 * 5xxx   服务端
 * 8xxx   外部依赖（LLM、向量存储、Redis）
 * </pre>
 */
@Getter
public enum CommonErrorCode implements ErrorCode {
    SUCCESS(0, "操作成功"),

    // ---- 参数 / 资源 ----
    PARAM_INVALID(1001, "参数校验失败"),
    RESOURCE_NOT_FOUND(1002, "资源不存在"),

    // ---- 认证 / 授权 ----
    UNAUTHORIZED(2001, "未认证"),
    FORBIDDEN(2002, "无权限"),

    // ---- 服务端 ----
    INTERNAL_ERROR(5001, "服务器内部错误"),

    // ---- 外部依赖 ----
    LLM_CALL_FAILED(8001, "LLM 调用失败"),
    EMBEDDING_FAILED(8002, "向量嵌入失败"),
    VECTOR_STORE_FAILED(8003, "向量存储操作失败"),
    REDIS_FAILED(8004, "Redis 操作失败"),
    ;

    private final int code;
    private final String message;

    CommonErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
