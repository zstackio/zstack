package com.zstack.utils.test;

import org.junit.Test;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.List;

public class TestGson {
    public static class B {
        String address;
    }
    
    public static class A {
        String name;
        List<B> bs = new ArrayList<B>(3);
    }
    
    @Test
    public void test() {
        B b = new B();
        b.address = "home";
        A a = new A();
        a.name = "hello";
        a.bs.add(b);
        a.bs.add(b);
        a.bs.add(b);
        
        System.out.println(JSONObjectUtil.toJsonString(a));
    }
}
