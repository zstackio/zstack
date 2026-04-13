package org.zstack.testlib.core;

import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;

import java.util.concurrent.CountDownLatch;

/**
 * A Completion that throws RuntimeException on success().
 * Used to test that AspectJ weaving catches the exception
 * and chain.next() still gets called.
 */
public class ThrowOnSuccessCompletion extends Completion {
    private final CountDownLatch latch;

    public ThrowOnSuccessCompletion(CountDownLatch latch) {
        super(null);
        this.latch = latch;
    }

    @Override
    public void success() {
        latch.countDown();
        throw new RuntimeException("intentional throw in success()");
    }

    @Override
    public void fail(ErrorCode errorCode) {
        latch.countDown();
    }
}
