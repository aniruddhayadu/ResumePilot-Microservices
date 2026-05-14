package com.resumepilot.template.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoggingAspectTest {

    @Test
    void logBeforeAndAfterAcceptJoinPoint() {
        JoinPoint joinPoint = mock(JoinPoint.class);
        Signature signature = mock(Signature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("getAllTemplates");
        LoggingAspect aspect = new LoggingAspect();

        assertThatCode(() -> aspect.logBefore(joinPoint)).doesNotThrowAnyException();
        assertThatCode(() -> aspect.logAfter(joinPoint)).doesNotThrowAnyException();
    }
}
