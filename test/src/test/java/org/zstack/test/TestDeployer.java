package org.zstack.test;

import org.junit.Before;
import org.junit.Test;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.DebugUtils;

/**
 */
public class TestDeployer {
    Deployer deployer;

    @Before
    public void setUp() throws Exception {
        String deployConfig = System.getProperty("config");
        DebugUtils.Assert(deployConfig != null, "you must specify deployer configure by -Dconfig=path");
        DBUtil.reDeployDB();
        deployer = new Deployer(deployConfig);
        deployer.build();
    }

    @Test
    public void test() {

    }
}
