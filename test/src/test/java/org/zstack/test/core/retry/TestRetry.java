package org.zstack.test.core.retry;

import junit.framework.Assert;
import org.junit.Test;
import org.zstack.core.retry.Retry;
import org.zstack.core.retry.RetryCondition;

import java.io.IOException;

public class TestRetry {
    int successAfter = 3;

    private String sayHello() {
        if (successAfter == 0) {
            return "hello";
        } else {
            successAfter--;
            throw new RuntimeException("on purpose");
        }
    }

    @Test
    public void test() {
        String ret = (String) new Retry() {

            {
                __name__ = "test";
            }

            @Override
            protected Object call() {
                return sayHello();
            }
        }.run();

        Assert.assertEquals("hello", ret);

        successAfter = 3;
        boolean s = false;
        try {
            ret = (String) new Retry() {
                @Override
                @RetryCondition(times = 1)
                protected Object call() {
                    return sayHello();
                }
            }.run();
        } catch (RuntimeException e) {
            s = true;
        }
        Assert.assertTrue(s);

        successAfter = 3;
        s = false;
        try {
            ret = (String) new Retry() {
                @Override
                @RetryCondition(times = 1, onExceptions = {IOException.class})
                protected Object call() {
                    return sayHello();
                }
            }.run();
        } catch (RuntimeException e) {
            s = true;
        }
        Assert.assertTrue(s);

        successAfter = 3;
        ret = (String) new Retry() {
            @Override
            @RetryCondition(interval = 2)
            protected Object call() {
                return sayHello();
            }
        }.run();

        Assert.assertEquals("hello", ret);
    }
}
