package org.zstack.test;

import com.google.common.primitives.UnsignedLong;
import com.googlecode.gentyref.GenericTypeReflector;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg;
import org.zstack.utils.FieldUtils;
import org.zstack.utils.FieldUtils.CollectionGenericType;
import org.zstack.utils.FieldUtils.GenericType;
import org.zstack.utils.FieldUtils.MapGenericType;
import org.zstack.utils.Utils;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 */
public class TestString {
    int offset = -2;
    public static class A {
        Map<String, Map<String, Map<List<String>, Map<Map<String, Long>, Map<Integer, Long>>>>> map;
        //Map map;
        String str;
    }

    private void dump(GenericType t) {
        if (!t.isInferred()) {
            return;
        }

        offset += 2;

        if (t.isMap()) {
            MapGenericType mt = t.cast();
            System.out.println(String.format("%skey: %s", StringUtils.repeat(" ", offset), mt.getKeyType().toString()));
            System.out.println(String.format("%svalue: %s", StringUtils.repeat(" ", offset), mt.getValueType().toString()));

            if (mt.getNestedGenericKey() != null) {
                dump(mt.getNestedGenericKey());
            }
            if (mt.getNestedGenericValue()!= null) {
                dump(mt.getNestedGenericValue());
            }
        } else if (t.isCollection()) {
            CollectionGenericType ct = t.cast();
            System.out.println(String.format("%slist: %s", StringUtils.repeat("", offset), ct.getValueType().toString()));
            if (ct.getNestedGenericValue() != null) {
                dump(ct);
            }
        }

        offset -= 2;
    }

    @Test
    public void test() throws InterruptedException {
        long s = System.currentTimeMillis();


        //long max = Long.MAX_VALUE - 100000;
        long max = 1000000;
        for (int i=0; i<max; i++) {
            //System.out.println(i%5);
            long z = i % 5;
        }

        long e = System.currentTimeMillis();

        System.out.println(String.format("%s", e - s));


        //System.out.println(n.plus(UnsignedLong.ONE).longValue());
    }
}
