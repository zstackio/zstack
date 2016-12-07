package org.zstack.test.configuration;

import org.junit.Test;
import org.zstack.rest.RestServer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestGenerateSDK {
    CLogger logger = Utils.getLogger(TestGenerateSDK.class);

    @Test
    public void test() {
        RestServer.generateJavaSdk();
    }
}
