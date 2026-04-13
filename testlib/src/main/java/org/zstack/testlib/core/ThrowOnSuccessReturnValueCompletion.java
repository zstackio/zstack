package org.zstack.testlib.core;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;

import java.util.concurrent.CountDownLatch;

/**
 * A ReturnValueCompletion that throws RuntimeException on success().
 * Used to test that AspectJ weaving catches the exception
 * and chain.next() still gets called.
 */
public class ThrowOnSuccessReturnValueCompletion extends ReturnValueCompletion<String> {
    private final CountDownLatch latch;

    public ThrowOnSuccessReturnValueCompletion(CountDownLatch latch) {
        super(null);
        this.latch = latch;
    }

    @Override
    public void success(String returnValue) {
        latch.countDown();
        throw new RuntimeException("intentional throw in ReturnValueCompletion.success()");
    }

    @Override
    public void fail(ErrorCode errorCode) {
        latch.countDown();
    }
}
