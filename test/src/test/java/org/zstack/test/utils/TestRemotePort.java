package org.zstack.test.utils;

import junit.framework.Assert;
import org.junit.Test;
import org.zstack.utils.network.NetworkUtils;

import java.util.concurrent.TimeUnit;

public class TestRemotePort {
    @Test
    public void test() {
        boolean open = NetworkUtils.isRemotePortOpen("127.0.0.1", 22, (int) TimeUnit.SECONDS.toMillis(1));
        Assert.assertTrue(open);
        open = NetworkUtils.isRemotePortOpen("127.0.0.1", 3, (int) TimeUnit.SECONDS.toMillis(1));
        Assert.assertFalse(open);
    }
}
