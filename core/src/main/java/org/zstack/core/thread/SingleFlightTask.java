package org.zstack.core.thread;

import org.zstack.header.core.AsyncBackup;
import org.zstack.header.core.Completion;

import java.util.function.Consumer;

public class SingleFlightTask extends AbstractChainTask {
    public SingleFlightTask(AsyncBackup one, AsyncBackup... others) {
        super(one, others);
    }

    public interface SingleFlightDone {
        void accept(SingleFlightTaskResult result);
    }

    @Override
    public String getSyncSignature() {
        return this.syncSignature;
    }

    private String syncSignature;
    private Consumer<Completion> consumer;
    private SingleFlightDone singleFlightDone;

    @Override
    public String getName() {
        return getSyncSignature();
    }

    public SingleFlightTask setSyncSignature(String syncSignature) {
        this.syncSignature = syncSignature;
        return this;
    }

    public SingleFlightTask run(Consumer<Completion> consumer) {
        this.consumer = consumer;
        return this;
    }

    public SingleFlightTask done(SingleFlightDone singleFlightDone) {
        this.singleFlightDone = singleFlightDone;
        return this;
    }

    protected void singleFlightDone(SingleFlightTaskResult taskResult) {
        singleFlightDone.accept(taskResult);
    }

    protected void start(Completion externalCompletion) {
        consumer.accept(externalCompletion);
    }
}
