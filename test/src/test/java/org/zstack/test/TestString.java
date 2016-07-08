package org.zstack.test;

import org.junit.Test;
import org.zstack.prometheus.PrometheusConfig;

/**
 */
public class TestString {
    @Test
    public void test() throws InterruptedException {
        System.out.println(PrometheusConfig.NewDefaultConfig().toString());
    }
}
