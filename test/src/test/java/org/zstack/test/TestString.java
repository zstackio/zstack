package org.zstack.test;

import org.apache.commons.beanutils.PropertyUtils;
import org.junit.Test;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.L2VlanNetworkInventory;
import org.zstack.rest.JsonSchemaBuilder;
import org.zstack.utils.gson.JSONObjectUtil;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class TestString {
    public static class TestJsonSchema {
        List lst = new ArrayList();
        Map<String, L2NetworkInventory> map = new HashMap<>();

        public List getLst() {
            return lst;
        }

        public void setLst(List lst) {
            this.lst = lst;
        }

        public Map<String, L2NetworkInventory> getMap() {
            return map;
        }

        public void setMap(Map<String, L2NetworkInventory> map) {
            this.map = map;
        }
    }

    @Test
    public void test() throws InterruptedException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        L2VlanNetworkInventory l2 = new L2VlanNetworkInventory();
        l2.setName("xx");
        l2.setVlan(10);

        TestJsonSchema s = new TestJsonSchema();
        s.lst.add(l2);
        s.map.put("l2", l2);

        Map m = new JsonSchemaBuilder(s).build();

        System.out.println(JSONObjectUtil.toJsonString(m));

        System.out.println(PropertyUtils.describe(s));
    }
}
