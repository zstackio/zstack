package org.zstack.core.asyncbatch;

import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.workflow.WhileCompletion;
import org.zstack.utils.DebugUtils;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by xing5 on 2017/3/5.
 */
public class While<T> {
    private Collection<T> items;
    private Do consumer;

    private int mode;
    private int step;

    private final int EACH = 1;
    private final int ALL = 2;
    private final int STEP = 3;

    public interface Do<T> {
        void accept(T item, WhileCompletion completion);
    }

    public While(Collection<T> items) {
        this.items = items;
    }

    public While each(Do<T> consumer) {
        mode = EACH;
        this.consumer = consumer;
        return this;
    }

    public While all(Do<T> consumer) {
        mode = ALL;
        this.consumer = consumer;
        return this;
    }

    private void run(Iterator<T> it, NoErrorCompletion completion) {
        if (!it.hasNext()) {
            completion.done();
            return;
        }

        T t = it.next();
        consumer.accept(t, new WhileCompletion(completion) {
            @Override
            public void allDone() {
                completion.done();
            }

            @Override
            public void done() {
                run(it, completion);
            }
        });
    }

    public While step(Do<T> consumer, int step) {
        if (step < 0) {
            throw new IllegalArgumentException(String.format("step must be greater than zero, got %s", step));
        }

        this.consumer = consumer;
        this.step = step;
        mode = STEP;
        return this;
    }

    public void run(NoErrorCompletion completion) {
        DebugUtils.Assert(consumer != null, "each() or all() or step() must be called before run()");

        if (mode == EACH) {
            run(items.iterator(), completion);
        } else if (mode == ALL) {
            runAll(completion);
        } else if (mode == STEP) {
            runStep(completion);
        } else {
            DebugUtils.Assert(false, "should be here");
        }
    }

    private void runStep(NoErrorCompletion completion) {
        int s = Math.min(step, items.size());

        Iterator<T> it = items.iterator();
        for (int i=0; i<s; i++) {
            runStep(it, completion);
        }
    }

    private void runStep(Iterator<T> it, NoErrorCompletion completion) {
        T t;
        synchronized (it) {
            if (!it.hasNext()) {
                completion.done();
                return;
            }

            t = it.next();
        }

        consumer.accept(t, new WhileCompletion(completion) {
            @Override
            public void allDone() {
                completion.done();
            }
            @Override
            public void done() {
                runStep(it, completion);
            }
        });
    }

    private void runAll(NoErrorCompletion completion) {
        AtomicInteger count = new AtomicInteger(items.size());

        if(count.intValue() == 0){
            completion.done();
            return;
        }
        for (T t : items) {
            consumer.accept(t, new WhileCompletion(completion) {
                @Override
                public void allDone() {
                    completion.done();
                }

                @Override
                public void done() {
                    if (count.decrementAndGet() == 0) {
                        completion.done();
                    }
                }
            });
        }
    }
}
