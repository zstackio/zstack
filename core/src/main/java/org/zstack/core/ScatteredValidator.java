package org.zstack.core;

import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by xing5 on 2017/5/2.
 */
public abstract class ScatteredValidator {
    protected static List<Method> methods;
    private static final CLogger logger = Utils.getLogger(ScatteredValidator.class);

    private static final String getClassNameForStatic() {
        return new Object() {
            String getClassName() {
                String className = this.getClass().getName();
                return className.substring(0, className.lastIndexOf('$'));
            }
        }.getClassName();
    }

    protected static void collectValidatorMethods(Class annotationClass, Class...argTypes) {
        if (argTypes == null) {
            argTypes = new Class[]{};
        }

        methods = new ArrayList<>();
        Set<Method> ms = Platform.getReflections().getMethodsAnnotatedWith(annotationClass);
        if (ms.isEmpty()){
            logger.warn(String.format("no validator found in %s", getClassNameForStatic()));
        } else {
            logger.debug(String.format("found validator %s",ms.iterator().next().getName()));
        }

        for (Method m : ms) {
            if (!Modifier.isStatic(m.getModifiers())) {
                throw new CloudRuntimeException(String.format("@%s %s.%s must be defined as static method", annotationClass, m.getDeclaringClass(), m.getName()));
            }

            if (m.getParameterCount() != argTypes.length) {
                throw new CloudRuntimeException(String.format("wrong argument list of the @%s %s.%s, %s arguments required" +
                        " but the method has %s arguments", annotationClass, m.getDeclaringClass(), m.getName(), argTypes.length,
                        m.getParameterCount()));
            }

            for (int i=0; i<argTypes.length; i++) {
                Class expectedType = argTypes[i];
                Class actualType = m.getParameterTypes()[i];

                if (expectedType != actualType) {
                    throw new CloudRuntimeException(String.format("wrong argument list of the @%s %s.%s. The argument[%s] is expected of type %s" +
                                    " but got type %s", annotationClass, m.getDeclaringClass(), m.getName(), i, expectedType, actualType));
                }
            }

            m.setAccessible(true);
            methods.add(m);
            logger.debug(String.format("add method validator %s",ms.iterator().next().getName()));
        }
    }

    protected void invokeValidatorMethods(Object...args) {
        logger.debug("start invoke validator");
        DebugUtils.Assert(methods != null, "call collectValidatorMethods in static block before calling any methods");
        for (Method m : methods) {
            try {
                m.invoke(null, args);
            } catch (IllegalAccessException e) {
                throw new CloudRuntimeException(e);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof OperationFailureException) {
                    throw (OperationFailureException)e.getCause();
                } else {
                    throw new CloudRuntimeException(e);
                }
            }
        }
    }
}
