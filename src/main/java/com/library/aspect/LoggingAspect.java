package com.library.aspect;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * Aspect-Oriented logging: cross-cutting logging of service-layer method calls,
 * their execution time and any thrown exceptions — without touching business code.
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger logger = LogManager.getLogger(LoggingAspect.class);

    /**
     * Matches every public method in the service layer.
     */
    @Pointcut("execution(public * com.library.service..*(..))")
    public void serviceMethods() {
    }

    /**
     * Logs entry, exit, elapsed time and exceptions around each service method.
     *
     * @param joinPoint the intercepted method call
     * @return the original method's return value
     * @throws Throwable if the underlying method throws
     */
    @Around("serviceMethods()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "." + joinPoint.getSignature().getName();
        long start = System.currentTimeMillis();
        logger.debug("[AOP] → {}()", method);
        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            logger.debug("[AOP] ← {}() completed in {}ms", method, elapsed);
            return result;
        } catch (Throwable ex) {
            logger.error("[AOP] ✗ {}() threw {}: {}", method,
                    ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }
}
