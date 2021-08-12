package org.zstack.core.singleflight;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;

import java.util.Collection;
import java.util.function.Consumer;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CompletionSingleFlight<K, V> extends AbstractSingleFlight<K, V> {
    public void execute(final K key, Consumer<ReturnValueCompletion<V>> getter, ReturnValueCompletion<V> completion) {
        if (!join(key, completion)) {
            onNextPending(key, getContext(key));
            return;
        }
        final SingleFlightContext context = getContext(key);
        onFirstStart(key, context);

        new CompletionConsumer<>(getter).accept(new ReturnValueCompletion<V>(completion) {
            @Override
            public void success(V v) {
                onSuccess(key, context, v);
                Collection<ReturnValueCompletion<V>> rcs = dequeAll(key);
                notifyResult(rcs, v);
            }
    
            @Override
            public void fail(ErrorCode errorCode) {
                onFail(key, context, errorCode);
                Collection<ReturnValueCompletion<V>> rcs = dequeAll(key);
                notifyFailure(rcs, errorCode);
            }
        });
    }

    protected void onFirstStart(K key, SingleFlightContext context) { }

    protected void onNextPending(K key, SingleFlightContext context) { }
    
    protected void onSuccess(K key, SingleFlightContext context, V v) { }

    protected void onFail(K key, SingleFlightContext context, ErrorCode errorCode) { }

    /**
     * This class cooperate with AsyncSafeAspect.aj to meet pointcut execution(* *.*(.., ReturnValueCompletion, ..))
     * Consumer<ReturnValueCompletion<V>> can not trigger pointcut of AsyncSafeAspect.aj because
     * the parameter "ReturnValueCompletion" is generic type
     */
    static class CompletionConsumer<V> {
        Consumer<ReturnValueCompletion<V>> consumer;
        public CompletionConsumer(Consumer<ReturnValueCompletion<V>> consumer) {
            this.consumer = consumer;
        }
        void accept(ReturnValueCompletion<V> completion) {
            consumer.accept(completion);
        }
    }
}
