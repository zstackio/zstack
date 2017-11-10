package org.zstack.testlib.util

class TProxy {
    private Map self = [:]
    private Object adaptee

    TProxy(Object adaptee) {
        this.adaptee = adaptee
        self["methodMissing"] = { String name, args ->
            return adaptee.invokeMethod(name, args)
        }
        self["propertyMissing"] = { String name ->
            return adaptee[name]
        }
    }

    void hookMethod(String name, Closure c) {
        self[name] = c
        //c.delegate = self
        //c.resolveStrategy = Closure.DELEGATE_FIRST
    }

    Object asType(Class clz) {
        return self.asType(clz)
    }
}
