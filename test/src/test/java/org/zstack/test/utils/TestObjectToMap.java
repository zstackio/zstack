package org.zstack.test.utils;

import junit.framework.Assert;
import org.junit.Test;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mingjian.deng on 17/1/14.
 */
public class TestObjectToMap {
    public static CLogger logger = Utils.getLogger(TestObjectToMap.class);
    @Test
    public void test() throws IllegalAccessException {
        logger.debug("start TestObjectToMap");
        Map<String, String> testMap = new HashMap<String, String>();
        testMap.put("test1", "a");
        testMap.put("test2", "b");
        Object obj = (Object)testMap;
        HashMap<String, String> a = (HashMap)obj;
        Assert.assertEquals("a", a.get("test1"));
        Assert.assertEquals("b", a.get("test2"));


        Map<String, Object> addons = new HashMap<String, Object>();
        addons.put("test1", testMap);
        obj = addons.get("test1");
        logger.debug(obj.getClass().getSimpleName());
        HashMap<String, String> obj_tmp = (HashMap)obj;
        String obj1 = obj_tmp.get("test2");
        logger.debug(obj1);
    }
}
