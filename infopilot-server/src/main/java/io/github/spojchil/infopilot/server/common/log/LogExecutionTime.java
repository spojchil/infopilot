package io.github.spojchil.infopilot.server.common.log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 方法执行时间日志。超过慢请求阈值时以 WARN 标记 {@code [SLOW]}。 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LogExecutionTime {

    /** 超过此阈值（毫秒）以 WARN 级别记录。 */
    long slowThresholdMs() default 1000;
}
