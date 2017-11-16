package org.zstack.testlib.util

import net.sf.cglib.proxy.Enhancer
import net.sf.cglib.proxy.MethodInterceptor
import net.sf.cglib.proxy.MethodProxy
import org.zstack.utils.FieldUtils
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

import java.lang.reflect.Method
import java.lang.reflect.Modifier

class TProxy {
    CLogger logger = Utils.getLogger(TProxy.class)

    private Map self = [:]
    private Object proxyedObject

    class ProxyHandler implements MethodInterceptor {
        @Override
        Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
            Closure call = self[method.name] as Closure

            if (call != null) {
                return call({
                    return methodProxy.invokeSuper(o, objects)
                }, *objects)
            } else {
                return methodProxy.invokeSuper(o, objects)
            }
        }
    }

    TProxy(Class adapteeClass) {
        Enhancer enhancer = new Enhancer()
        enhancer.setSuperclass(adapteeClass)
        enhancer.setCallback(new ProxyHandler())
        proxyedObject = enhancer.create()
    }

    TProxy(Object adaptee) {
        assert adaptee != null : "call TProxy(Class adapteeClass) instead"

        Enhancer enhancer = new Enhancer()
        enhancer.setSuperclass(adaptee.getClass())
        enhancer.setCallback(new ProxyHandler())
        try {
            proxyedObject = enhancer.create()
            if (adaptee != null) {
                FieldUtils.getAllFields(adaptee.getClass()).each { f ->
                    if (Modifier.isStatic(f.modifiers) || Modifier.isFinal(f.modifiers)) {
                        return
                    }

                    f.setAccessible(true)
                    f.set(proxyedObject, f.get(adaptee))
                }
            }
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("Superclass has no null constructors" )) {
                throw new Exception("the class ${adaptee.getClass()} has no non-argument constructor", e)
            }

            throw e
        }
    }

    void hookMethod(String name, Closure c) {
        self[name] = c
    }

    Object asType(Class clz) {
        return proxyedObject
    }
}
