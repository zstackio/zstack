package org.zstack.test.utils;

import junit.framework.Assert;
import org.junit.Test;
import org.zstack.utils.FieldUtils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 */
public class TestGetGenericType {
    List<String> list;
    Map<String, Integer> map;

    List list1;
    Map map1;

    @Test
    public void test() throws NoSuchFieldException {
        Field f = TestGetGenericType.class.getDeclaredField("list");
        Class type = FieldUtils.getGenericType(f);
        Assert.assertEquals(String.class, type);

        f = TestGetGenericType.class.getDeclaredField("map");
        type = FieldUtils.getGenericType(f);
        Assert.assertEquals(Integer.class, type);

        f = TestGetGenericType.class.getDeclaredField("list1");
        type = FieldUtils.getGenericType(f);
        Assert.assertNull(type);

        f = TestGetGenericType.class.getDeclaredField("map1");
        type = FieldUtils.getGenericType(f);
        Assert.assertNull(type);
    }
}
