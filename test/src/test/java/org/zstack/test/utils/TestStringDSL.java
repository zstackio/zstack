package org.zstack.test.utils;

import org.junit.Test;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.utils.StringDSL.s;

/**
 */
public class TestStringDSL {
    CLogger logger = Utils.getLogger(TestStringDSL.class);

    @Test
    public void test() {
        String ret = s(
                "This is a {0}\n",
                "it's very interesting {1}\n",
                "you will see how I use {it}\n"
        ).format("a string", "string");

        logger.debug(ret);

        String str = "123M";
        logger.debug(str.substring(0, str.length() - 1));
    }
}
