package org.zstack.test.utils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class TestSubList {
    @Test
    public void test() {
        List<Integer> lst = new ArrayList<Integer>();
        lst.add(1);
        lst.add(2);
        lst.add(3);
        lst.add(4);
        lst.add(5);
        lst.add(6);


        List<Integer> sub = lst.subList(0, 3);
        List n = new ArrayList();
        n.addAll(sub);
        sub.clear();
        System.out.println(lst.toString());
        System.out.println(n.toString());
    }
}
