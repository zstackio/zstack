package org.zstack.testlib.util

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

class TProxy {
    private Map self = [:]
    private Object adaptee

    class ProxyHandler implements InvocationHandler {
        @Override
        Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Closure call = self[method.name] as Closure

            if (call != null) {
                return call(*args)
            } else {
                return method.invoke(adaptee, args)
            }
        }
    }

    TProxy(Object adaptee) {
        this.adaptee = adaptee
        /*
        self["methodMissing"] = { String name, args ->
            return adaptee.invokeMethod(name, args)
        }
        self["propertyMissing"] = { String name ->
            return adaptee[name]
        }
        */
    }

    void passThrough(String name, Object...args) {
        adaptee.invokeMethod(name, args)
    }

    void hookMethod(String name, Closure c) {
        self[name] = c
        //c.delegate = self
        //c.resolveStrategy = Closure.DELEGATE_FIRST
    }

    Object toProxy() {
        return java.lang.reflect.Proxy.newProxyInstance(adaptee.getClass().getClassLoader(), [adaptee.getClass()] as Class<?>[], new ProxyHandler())
    }

    Object asType(Class clz) {
        return java.lang.reflect.Proxy.newProxyInstance(adaptee.getClass().getClassLoader(), [clz] as Class<?>[], new ProxyHandler())
        //return self.asType(clz)
    }
}
