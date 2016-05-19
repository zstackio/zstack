package com.zstack.utils.test;

import junit.framework.Assert;
import org.apache.commons.net.util.SubnetUtils;
import org.junit.Test;
import org.zstack.utils.network.NetworkUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 8/11/2015.
 */
public class TestEval {
    public static class A {
    }

    @Test
    public void test() throws IOException {
        List<String> lst = list("CloudBus.serverIp.1", "CloudBus.serverIp.5", "CloudBus.serverIp.2", "CloudBus.serverIp.0");
        Collections.sort(lst);
        System.out.println(lst);

        System.out.println(NetworkUtils.isNetmask("255.255.255.0"));
        System.out.println(NetworkUtils.isNetmask("255.255.255.088"));
        System.out.println(NetworkUtils.isNetmaskExcept("0.0.0.0", "0.0.0.0"));

        SubnetUtils sub = new SubnetUtils("192.168.0.10/16");
        System.out.println(String.format("11111 %s", sub.getInfo().isInRange("192.168.0.1")));

        sub = new SubnetUtils("192.168.55.10/24");
        System.out.println(String.format("22222 %s", sub.getInfo().isInRange("192.168.0.1")));

        A a1 = new A();
        A a2 = new A();
        A a3 = new A();
        System.out.println(a1.getClass().hashCode());
        System.out.println(a2.getClass().hashCode());
        System.out.println(a3.getClass().hashCode());
        Assert.assertTrue(a1.getClass() == a2.getClass());
        Assert.assertTrue(a2.getClass() == a3.getClass());
    }
}
