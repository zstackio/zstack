package org.zstack.test;

import org.junit.Test;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.BeanUtils.getProperty;
import static org.zstack.utils.BeanUtils.setProperty;

/**
 */
public class TestString {
    CLogger logger = Utils.getLogger(TestString.class);

    @Test
    public void test() throws InterruptedException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Map m = new HashMap<>();
        List lst = new ArrayList();
        lst.add("abc");
        m.put("lst", lst);

        setProperty(m, "lst[0]", "E");
        System.out.println(getProperty(m, "lst[0]"));
    }
}
