package org.zstack.test;

import org.junit.Test;
import org.zstack.utils.TypeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.InvocationTargetException;

/**
 */
public class TestString {
    CLogger logger = Utils.getLogger(TestString.class);

    @Test
    public void test() throws InterruptedException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        assert TypeUtils.stringToValue("1.0", Integer.class) == 1;
        assert TypeUtils.stringToValue("1.0000000", Integer.class) == 1;
        assert TypeUtils.stringToValue("1.2", Integer.class) == 1;
        assert TypeUtils.stringToValue("1.3", Long.class) == 1L;
        assert TypeUtils.stringToValue("1.3", Float.class) == 1.3F;
        assert TypeUtils.stringToValue("1.3", Double.class) == 1.3D;
        assert TypeUtils.stringToValue("false", Boolean.class) == false;
        assert TypeUtils.stringToValue("true", Boolean.class) == true;
        assert TypeUtils.stringToValue("true", String.class).equals("true");
    }
}
