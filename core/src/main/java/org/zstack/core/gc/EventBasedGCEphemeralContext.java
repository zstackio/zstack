package org.zstack.core.gc;

/**
 * Created by frank on 8/5/2015.
 */
public class EventBasedGCEphemeralContext<T> extends AbstractEventBasedGCContext<T> {
    private GCRunner runner;

    public EventBasedGCEphemeralContext() {
    }

    public EventBasedGCEphemeralContext(EventBasedGCEphemeralContext other) {
        super(other);
        runner = other.runner;
    }

    public GCRunner getRunner() {
        return runner;
    }

    public void setRunner(GCRunner runner) {
        this.runner = runner;
    }
}
