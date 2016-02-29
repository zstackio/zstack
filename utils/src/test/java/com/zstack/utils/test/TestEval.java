package com.zstack.utils.test;

import org.junit.Test;

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
    }
}
