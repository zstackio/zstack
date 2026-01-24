package org.zstack.core.telemetry;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.zstack.core.Platform;
import org.zstack.header.core.Traced;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Aspect
@Component
public class TracingAspect {
    private static final CLogger logger = Utils.getLogger(TracingAspect.class);

    /**
     * Parameter names that may contain sensitive data. Values are not recorded to span attributes,
     * only "<redacted>" is set to avoid leaking passwords, tokens, etc.
     *
     * Note: We use parameter name-based filtering instead of LogSafeGson's @NoLogging annotation
     * mechanism for the following reasons:
     * 1. LogSafeGson works on class fields (annotated with @NoLogging), but here we handle method
     *    parameters which cannot be annotated directly.
     * 2. LogSafeGson requires objects to implement Serializable or Message interface, but method
     *    parameters can be of any type (primitives, String, custom objects, etc.).
     * 3. LogSafeGson involves JSON serialization which is too heavyweight for tracing scenarios
     *    where we need lightweight, fast attribute recording.
     * 4. Parameter name matching is simpler and more direct for this use case, avoiding the need
     *    to introspect object structures or perform serialization.
     */
    private static final Set<String> SENSITIVE_PARAM_NAMES = Collections.unmodifiableSet(
            Arrays.asList(
                    "password", "passwd", "pwd",
                    "token", "accesstoken", "refreshtoken", "apitoken",
                    "apikey", "api_key", "secret", "secretkey", "secret_key",
                    "credential", "credentials", "authorization", "auth",
                    "privatekey", "private_key", "sessionid", "session_id",
                    "consolepassword", "console_password"
            ).stream().map(String::toLowerCase).collect(Collectors.toSet()));

    private volatile TelemetryFacade telemetryFacade;

    private TelemetryFacade getTelemetryFacade() {
        if (telemetryFacade == null && TelemetryGlobalProperty.ENABLED) {
            synchronized (this) {
                if (telemetryFacade == null) {
                    try {
                        telemetryFacade = Platform.getComponentLoader().getComponent(TelemetryFacade.class);
                    } catch (Exception e) {
                        logger.trace("TelemetryFacade not available", e);
                    }
                }
            }
        }
        return telemetryFacade;
    }

    private boolean isTelemetryEnabled() {
        return TelemetryGlobalProperty.ENABLED && getTelemetryFacade() != null && getTelemetryFacade().isEnabled();
    }

    @Pointcut("@annotation(org.zstack.header.core.Traced)")
    public void tracedMethod() {
    }

    @Around("tracedMethod()")
    public Object aroundTracedMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!isTelemetryEnabled()) {
            return joinPoint.proceed();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Traced traced = method.getAnnotation(Traced.class);

        String operationName = buildOperationName(traced, signature, joinPoint);

        Span span = getTelemetryFacade().getTracer()
                .spanBuilder(operationName)
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute("code.function", method.getName())
                .setAttribute("code.namespace", joinPoint.getTarget().getClass().getName())
                .startSpan();

        addCustomAttributes(span, traced);

        if (traced.recordParameters()) {
            addParameterAttributes(span, signature, joinPoint.getArgs());
        }

        try (Scope scope = span.makeCurrent()) {
            Object result = joinPoint.proceed();
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Throwable t) {
            span.recordException(t);
            span.setStatus(StatusCode.ERROR, t.getMessage());
            throw t;
        } finally {
            span.end();
        }
    }

    private String buildOperationName(Traced traced, MethodSignature signature, ProceedingJoinPoint joinPoint) {
        if (traced.operationName() != null && !traced.operationName().isEmpty()) {
            return traced.operationName();
        }
        return joinPoint.getTarget().getClass().getSimpleName() + "." + signature.getName();
    }

    private void addCustomAttributes(Span span, Traced traced) {
        String attributes = traced.attributes();
        if (attributes == null || attributes.isEmpty()) {
            return;
        }

        for (String pair : attributes.split(",")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                span.setAttribute(kv[0].trim(), kv[1].trim());
            }
        }
    }

    private void addParameterAttributes(Span span, MethodSignature signature, Object[] args) {
        String[] paramNames = signature.getParameterNames();
        if (paramNames == null || args == null) {
            return;
        }

        for (int i = 0; i < Math.min(paramNames.length, args.length); i++) {
            if (args[i] != null) {
                String paramName = paramNames[i];
                String value;
                if (isSensitiveParameter(paramName)) {
                    value = "<redacted>";
                } else {
                    value = safeToString(args[i]);
                    if (value.length() > 256) {
                        value = value.substring(0, 256) + "...";
                    }
                }
                span.setAttribute("param." + paramName, value);
            }
        }
    }

    private boolean isSensitiveParameter(String paramName) {
        return paramName != null && SENSITIVE_PARAM_NAMES.contains(paramName.toLowerCase());
    }

    private String safeToString(Object obj) {
        try {
            if (obj instanceof String) {
                return (String) obj;
            }
            if (obj instanceof Number || obj instanceof Boolean) {
                return obj.toString();
            }
            return obj.getClass().getSimpleName() + "@" + System.identityHashCode(obj);
        } catch (Exception e) {
            return "<error>";
        }
    }
}
