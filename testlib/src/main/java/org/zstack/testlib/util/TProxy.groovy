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
                if (call.maximumNumberOfParameters == objects.length) {
                    return call(*objects)
                } else {
                    return call({
                        return methodProxy.invokeSuper(o, objects)
                    }, *objects)
                }
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

    TProxy mockMethod(String name, Closure c) {
        self[name] = c
        return this
    }

    Object asType(Class clz) {
        return proxyedObject
    }

    FieldProtector protect(bean, String...fnames) {
        return new FieldProtector(bean, fnames)
    }

    class FieldProtector {
        def bean
        Map<String, Object> fields = [:]

        FieldProtector(bean, String...fnames) {
            this.bean = bean
            fnames.each {
                fields[it] = bean[it]
            }
        }

        void recover() {
            fields.each {k ,v ->
                bean[k] = v
            }
        }
    }
}
