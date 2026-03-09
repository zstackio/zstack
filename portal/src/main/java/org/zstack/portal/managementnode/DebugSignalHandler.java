package org.zstack.portal.managementnode;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Handles Unix signals (e.g., SIGUSR2) via reflection to avoid
 * compile-time dependency on sun.misc.Signal (internal JDK API).
 * The sun.misc.Signal API is provided by jdk.unsupported module
 * and remains the only way to handle custom Unix signals in Java.
 */
public class DebugSignalHandler {
    private final ManagementNodeManagerImpl impl;

    public static void listenTo(String name, ManagementNodeManagerImpl impl) {
        try {
            Class<?> signalClass = Class.forName("sun.misc.Signal");
            Class<?> handlerClass = Class.forName("sun.misc.SignalHandler");

            Object signal = signalClass.getConstructor(String.class).newInstance(name);

            DebugSignalHandler handler = new DebugSignalHandler(impl);
            Object proxy = Proxy.newProxyInstance(
                    handlerClass.getClassLoader(),
                    new Class<?>[]{handlerClass},
                    (p, method, args) -> {
                        if ("handle".equals(method.getName())) {
                            handler.handle(args[0]);
                        }
                        return null;
                    }
            );

            Method handleMethod = signalClass.getMethod("handle", signalClass, handlerClass);
            handleMethod.invoke(null, signal, proxy);
        } catch (Exception e) {
            // Signal handling not available on this JVM — silently skip
        }
    }

    private DebugSignalHandler(ManagementNodeManagerImpl impl) {
        this.impl = impl;
    }

    private void handle(Object signal) {
        if (signal.toString().trim().equals("SIGUSR2")) {
            impl.setSigUsr2();
        }
    }
}
