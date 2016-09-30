package org.zstack.utils.ssh;

/**
 */
public class SshException extends RuntimeException {
    public SshException(String msg) {
        super(msg);
    }

    public SshException(String msg, Throwable t) {
        super(msg, t);
    }

    public SshException(Throwable t) {
        super(t);
    }
}
