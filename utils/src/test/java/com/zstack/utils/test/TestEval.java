package com.zstack.utils.test;

import org.junit.Test;

import java.io.IOException;

/**
 * Created by frank on 8/11/2015.
 */
public class TestEval {
    public static class A {
    }

    @Test
    public void test() throws IOException, ClassNotFoundException {
        System.out.println(A.class.getName());

        Class.forName("com.zstack.utils.test.TestEval$A");
    }
}
