package org.zstack.test.core;

import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 12:18 AM
 * To change this template use File | Settings | File Templates.
 */
public class TestGetManagementIp {
    CLogger logger = Utils.getLogger(TestGetManagementIp.class);

    @Test
    public void test() throws Exception {
        String ip = Platform.getManagementServerIp();
        logger.debug(String.format("management ip: %s", ip));
    }
}
