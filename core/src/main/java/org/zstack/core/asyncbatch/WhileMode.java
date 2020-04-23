package org.zstack.core.asyncbatch;

public enum WhileMode {
    EACH(1), ALL(2), STEP(3);
    private final int VALUE;

    WhileMode(int VALUE) {
        this.VALUE = VALUE;
    }

    public int getNumVal() {
        return VALUE;
    }
}
