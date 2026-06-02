package io.github.spojchil.infopilot.server.common.response;

/** 错误码约定。各模块可扩展新的实现。 */
public interface ErrorCode {

    int getCode();

    String getMessage();
}
