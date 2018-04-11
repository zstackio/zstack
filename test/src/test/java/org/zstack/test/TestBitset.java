package org.zstack.test;

import org.junit.Test;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class TestBitset {

    public static class A {

    }


    @Test
    public void test() {
        A a = new A() {
        };

        System.out.println(String.format("%s, %s", A.class.isMemberClass(), a.getClass().isMemberClass()));
    }

}
