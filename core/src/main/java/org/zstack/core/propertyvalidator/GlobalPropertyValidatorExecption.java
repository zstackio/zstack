package org.zstack.core.propertyvalidator;

public class GlobalPropertyValidatorExecption extends RuntimeException {
    public GlobalPropertyValidatorExecption(String msg, Throwable t) {
        super(msg, t);
    }

    public GlobalPropertyValidatorExecption(String msg) {
        super(msg);
    }

    public GlobalPropertyValidatorExecption(Throwable t) {
        super(t);
    }

}

