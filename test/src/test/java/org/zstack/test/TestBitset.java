package org.zstack.test;

import org.junit.Test;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class TestBitset {

    @Test
    public void test() {
        int num = 100;
        BitSet bit = new BitSet(num);
        //bit.set(0, num);
        Random r = new Random();
        long s = System.currentTimeMillis();
        for (int i = 0; i < num; i++) {
            bit.set(r.nextInt(num));
        }
        int f = bit.cardinality();
        System.out.println(bit.cardinality());
        Set<Integer> free = new HashSet<Integer>(num / 2);
        int j = 0;
        for (int i = 0; i < num - f; i++) {
            int a = bit.nextClearBit(j);
            j = a + 1;
            free.add(a);
        }
        long e = System.currentTimeMillis();
        System.out.println(e - s);
        System.out.println(free.size());
    }

}
