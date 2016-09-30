package org.zstack.test.utils;

import org.junit.Test;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.StringDSL.s;

/**
 */
public class TestMapDSL {
    CLogger logger = Utils.getLogger(TestMapDSL.class);

    private String properties(int port) {
        return s(
                "-DRESTFacade.port={port} ",
                "-DKVMHostFactory.agentPort={port} ",
                "-DSftpBackupStorageFactory.agentPort={port} ",
                "-DVirtualRouterManager.agentPort={port} ",
                "-DManagementServerConsoleProxyBackend.agentPort={port} "
        ).formatByMap(map(e("port", port)));
    }

    @Test
    public void test() {
        Map<String, Integer> ret = map(e("port", 1));

        logger.debug(properties(888));
    }
}
