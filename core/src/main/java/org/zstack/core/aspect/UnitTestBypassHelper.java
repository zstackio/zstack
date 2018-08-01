package org.zstack.core.aspect;

import org.aspectj.lang.JoinPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class UnitTestBypassHelper {
    private static final CLogger logger = Utils.getLogger(UnitTestBypassHelper.class);

    private static Map<String, Consumer<JoinPoint>> consumers = new HashMap<>();

    public static Runnable registerBypassConsumer(String signature, Consumer<JoinPoint> consumer) {
        consumers.put(signature, consumer);
        return () -> consumers.remove(signature);
    }

    static void callConsumer(JoinPoint jp) {
        String name = String.format("%s.%s", jp.getSignature().getDeclaringType().getName(), jp.getSignature().getName());
        Consumer<JoinPoint> consumer = consumers.get(name);
        if (consumer != null) {
            consumer.accept(jp);
        }
    }
}
