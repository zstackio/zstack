package org.zstack.core.singleflight;

import org.zstack.core.thread.AsyncThread;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.zstack.core.Platform.operr;

/**
 * Created by Wenhao.Zhang on 20/11/10
 */
public abstract class AbstractSingleFlight<V> {
    protected final Map<Object, Collection<ReturnValueCompletion<V>>> calls =
        new HashMap<>();
    
    protected Collection<ReturnValueCompletion<V>> dequeAll(Object key) {
        synchronized (this) {
            return calls.remove(key);
        }
    }
    
    protected boolean isFirst(Object key, ReturnValueCompletion<V> completion) {
        boolean first;
    
        synchronized (this) {
            // update the queue (not in a small function like 'dequeAll' to avoid AspectJ)
            final Collection<ReturnValueCompletion<V>> rcs = calls.getOrDefault(key, new ArrayList<>());
            first = rcs.isEmpty();
        
            rcs.add(completion);
            calls.put(key, rcs);
        }
        
        return first;
    }
    
    @AsyncThread
    protected void success(ReturnValueCompletion<V> consumer, final V v) {
        consumer.success(v);
    }
    
    @AsyncThread
    protected void fail(ReturnValueCompletion<V> consumer, ErrorCode errorCode) {
        consumer.fail(errorCode);
    }
    
    @AsyncThread
    protected void fail(ReturnValueCompletion<V> consumer, Exception ex) {
        consumer.fail(operr(ex.getMessage()));
    }
    
    protected void notifyResult(Collection<ReturnValueCompletion<V>> consumers, final V v) {
        consumers.forEach(c -> success(c, v));
    }
    
    protected void nofityFailure(Collection<ReturnValueCompletion<V>> consumers, ErrorCode errorCode) {
        consumers.forEach(c -> fail(c, errorCode));
    }
    
    protected void nofityFailure(Collection<ReturnValueCompletion<V>> consumers, Exception ex) {
        consumers.forEach(c -> fail(c, ex));
    }
}
