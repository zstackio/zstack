package org.zstack.test.utils;

import junit.framework.Assert;
import org.junit.Test;
import org.zstack.utils.MapDSL;

import java.util.Map;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 */
public class TestNestedGsonMap {

    @Test
    public void test() throws InterruptedException {
        Map obj = map(e("a",
                map(e("b",
                        map(e("c", 10))),
                        e("d", map(e("h", 100)
                        ))
                )));


        Integer num = MapDSL.findValue(obj, "h");
        Assert.assertEquals(Integer.valueOf(100), num);
        Map b = MapDSL.findValue(obj, "b");
        num = (Integer) b.get("c");
        Assert.assertEquals(Integer.valueOf(10), num);
    }
}
