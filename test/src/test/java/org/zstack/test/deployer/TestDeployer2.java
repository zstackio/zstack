package org.zstack.test.deployer;

import org.junit.Test;
import org.zstack.test.DBUtil;

public class TestDeployer2 {

    @Test
    public void test() {
        DBUtil.reDeployDB();
        Deployer deployer = new Deployer("deployerXml/deployer/sample.xml");
        deployer.build();
    }
}
