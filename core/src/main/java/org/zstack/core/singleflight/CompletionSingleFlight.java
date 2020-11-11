package org.zstack.core.singleflight;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;

import java.util.Collection;
import java.util.function.Consumer;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CompletionSingleFlight<V> extends AbstractSingleFlight<V> {
    
    public void execute(Object key, Consumer<ReturnValueCompletion<V>> getter, ReturnValueCompletion<V> completion) {
        if (!isFirst(key, completion)) {
            return;
        }
        
        try {
            getter.accept(new ReturnValueCompletion<V>(completion) {
                @Override
                public void success(V v) {
                    Collection<ReturnValueCompletion<V>> rcs = dequeAll(key);
                    notifyResult(rcs, v);
                }
        
                @Override
                public void fail(ErrorCode errorCode) {
                    Collection<ReturnValueCompletion<V>> rcs = dequeAll(key);
                    nofityFailure(rcs, errorCode);
                }
            });
        } catch (RuntimeException e) {
            Collection<ReturnValueCompletion<V>> rcs = dequeAll(key);
            this.nofityFailure(rcs, e);
        }
    }
}
