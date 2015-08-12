package com.zstack.utils.test;

import groovy.util.Eval;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by frank on 8/11/2015.
 */
public class TestEval {
    @Test
    public void test() throws IOException {
        int num = (Integer) Eval.me("2 * 10 + 6");
        System.out.print(num);
    }
}
