package org.zstack.core.asyncbatch;

public final class Iteration<I> {
    private final int index;
    private final I item;

    Iteration(int index, I item) {
        this.index = index;
        this.item = item;
    }

    public int index() {
        return index;
    }

    public I item() {
        return item;
    }
}
