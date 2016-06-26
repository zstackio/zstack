package org.zstack.header.core;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 */
public abstract class AbstractCompletion implements AsyncBackup {
    protected List<AsyncBackup> backups;
    private final AtomicBoolean successCalled = new AtomicBoolean(false);
    private final AtomicBoolean failCalled = new AtomicBoolean(false);

    public final List<AsyncBackup> getBackups() {
        return backups;
    }

    public final void setBackups(List<AsyncBackup> backups) {
        this.backups = backups;
    }

    protected AbstractCompletion(AsyncBackup...backups) {
        this.backups = Arrays.asList(backups);
    }

    protected AbstractCompletion() {
        backups = null;
    }

    public final AtomicBoolean getSuccessCalled() {
        return successCalled;
    }

    public final AtomicBoolean getFailCalled() {
        return failCalled;
    }
}
