package io.github.spojchil.infopilot.server.common.log;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * {@link LogExecutionTime} 注解的 AOP 切面。
 *
 * <p>记录格式：{@code [Exec] 类名.方法名 | OK/FAIL | {N}ms [SLOW]}
 */
@Slf4j
@Aspect
@Component
public class LogExecutionTimeAspect {

    @Around("@annotation(annotation)")
    public Object measure(ProceedingJoinPoint joinPoint, LogExecutionTime annotation)
            throws Throwable {
        long start = System.currentTimeMillis();
        String label =
                "[Exec] "
                        + joinPoint.getSignature().getDeclaringType().getSimpleName()
                        + "."
                        + joinPoint.getSignature().getName();
        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed >= annotation.slowThresholdMs()) {
                log.warn("{} | OK | {}ms [SLOW]", label, elapsed);
            } else {
                log.debug("{} | OK | {}ms", label, elapsed);
            }
            return result;
        } catch (Throwable e) {
            long elapsed = System.currentTimeMillis() - start;
            log.warn("{} | FAIL | {}ms | {}", label, elapsed, e.getClass().getSimpleName());
            throw e;
        }
    }
}
