package org.zstack.core.singleflight;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.ReturnValueCompletion;

import java.util.Collection;
import java.util.concurrent.Callable;

/**
 * AsyncSingleFlight implemented an asynchronous single flight that won't
 * block the caller.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AsyncSingleFlight<V> extends AbstractSingleFlight<V> {

    public void execute(Object key, Callable<V> callable, ReturnValueCompletion<V> completion) {
        if (!isFirst(key, completion)) {
            return;
        }

        try {
            V v = callable.call();
            Collection<ReturnValueCompletion<V>> rcs = dequeAll(key);
            notifyResult(rcs, v);
        } catch (Exception ex) {
            Collection<ReturnValueCompletion<V>> rcs = dequeAll(key);
            nofityFailure(rcs, ex);
        }
    }
}
