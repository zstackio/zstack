package org.zstack.core.singleflight;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.core.ReturnValueCompletion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.zstack.core.Platform.operr;

/**
 * AsyncSingleFlight implemented an asynchronous single flight that won't
 * block the caller.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AsyncSingleFlight<V> {
    @Autowired
    private ErrorFacade errf;

    private final Map<Object, Collection<ReturnValueCompletion<V>>> calls =
            new HashMap<>();

    private Collection<ReturnValueCompletion<V>> dequeAll(Object key) {
        synchronized (this) {
            return calls.remove(key);
        }
    }

    public void execute(Object key, Callable<V> callable, ReturnValueCompletion<V> completion) {
        boolean isFirst;

        synchronized (this) {
            // update the queue (not in a small function like 'dequeAll' to avoid AspectJ)
            final Collection<ReturnValueCompletion<V>> rcs = calls.getOrDefault(key, new ArrayList<>());
            isFirst = rcs.isEmpty();

            rcs.add(completion);
            calls.put(key, rcs);
        }

        if (!isFirst) {
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

    @AsyncThread
    private void success(ReturnValueCompletion<V> consumer, final V v) {
        consumer.success(v);
    }

    @AsyncThread
    private void fail(ReturnValueCompletion<V> consumer, Exception ex) {
        consumer.fail(operr(ex.getMessage()));
    }

    private void notifyResult(Collection<ReturnValueCompletion<V>> consumers, final V v) {
        consumers.forEach(c -> success(c, v));
    }

    private void nofityFailure(Collection<ReturnValueCompletion<V>> consumers, Exception ex) {
        consumers.forEach(c -> fail(c, ex));
    }
}
