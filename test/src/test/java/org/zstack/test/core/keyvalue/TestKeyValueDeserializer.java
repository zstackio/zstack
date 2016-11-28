package org.zstack.test.core.keyvalue;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.keyvalue.KeyValueEntityProxy;
import org.zstack.core.keyvalue.KeyValueFacade;
import org.zstack.core.keyvalue.KeyValueQuery;
import org.zstack.core.keyvalue.Op;
import org.zstack.header.core.keyvalue.KeyValueEntity;
import org.zstack.test.BeanConstructor;
import org.zstack.test.DBUtil;

import java.util.*;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 */
public class TestKeyValueDeserializer {
    public static class A implements KeyValueEntity {
        int a1 = 10;
        int b1 = 100;
        String c2 = "hello world";
        Map<String, String> dict = map(e("last name", "zhang"), e("first name", "xin"));
        List<Integer> lst = Arrays.asList(1, 2, 3, 4, 5);
        A child;
        Map<String, A> dict2;
        List<A> lst2;
        String uuid = Platform.getUuid();

        public int getA1() {
            return a1;
        }

        public void setA1(int a1) {
            this.a1 = a1;
        }

        public int getB1() {
            return b1;
        }

        public void setB1(int b1) {
            this.b1 = b1;
        }

        public String getC2() {
            return c2;
        }

        public void setC2(String c2) {
            this.c2 = c2;
        }

        public Map<String, String> getDict() {
            return dict;
        }

        public void setDict(Map<String, String> dict) {
            this.dict = dict;
        }

        public List<Integer> getLst() {
            return lst;
        }

        public void setLst(List<Integer> lst) {
            this.lst = lst;
        }

        public A getChild() {
            return child;
        }

        public void setChild(A child) {
            this.child = child;
        }

        public Map<String, A> getDict2() {
            return dict2;
        }

        public void setDict2(Map<String, A> dict2) {
            this.dict2 = dict2;
        }

        public List<A> getLst2() {
            return lst2;
        }

        public void setLst2(List<A> lst2) {
            this.lst2 = lst2;
        }

        @Override
        public String getUuid() {
            return uuid;
        }
    }

    ComponentLoader loader;
    KeyValueFacade kvf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        kvf = loader.getComponent(KeyValueFacade.class);
    }

    @Test
    public void test() {
        A a = new A();
        a.child = new A();
        a.dict2 = new HashMap<String, A>();
        a.dict2.put("aa", new A());
        a.dict2.put("aaa", new A());
        a.lst2 = new ArrayList<A>();
        a.lst2.add(new A());
        a.lst2.add(new A());

        KeyValueEntityProxy<A> proxy = new KeyValueEntityProxy<A>(A.class);
        A entity = proxy.getProxyEntity();
        entity.getA1();
        entity.getC2();
        entity.getChild().getChild().getA1();
        entity.getChild().getDict2().get(null).getDict2().get("hi").getChild().getA1();
        entity.getLst().get(-1);
        entity.getLst2().get(1).getA1();
        entity.getDict().get("last name");
        entity.getDict2().get(null).getB1();
        for (String p : proxy.getPaths()) {
            System.out.println(p);
        }

        kvf.persist(a);

        KeyValueQuery<A> q = new KeyValueQuery(A.class);
        q.and(q.entity().getA1(), Op.EQ, 10);
        A ret = q.find();
        Assert.assertEquals(a.getUuid(), ret.getUuid());

        a.setA1(99);
        kvf.update(a);
        a = kvf.find(a.getUuid());
        Assert.assertEquals(99, a.getA1());
        kvf.delete(a);
    }
}
