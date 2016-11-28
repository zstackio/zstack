package org.zstack.test.identity;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.identity.UserGroupVO;
import org.zstack.header.identity.UserVO;
import org.zstack.header.identity.UserVO_;
import org.zstack.test.Api;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

public class TestAttachUserToGroup {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/identity/TestAttachUserToGroup.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws InterruptedException {
        SimpleQuery<UserGroupVO> q = dbf.createQuery(UserGroupVO.class);
        q.add(UserVO_.name, Op.EQ, "TestGroup1");
        UserGroupVO vo = q.find();
        SimpleQuery<UserVO> uq = dbf.createQuery(UserVO.class);
        uq.add(UserVO_.name, Op.EQ, "TestUser");
        UserVO uvo = uq.find();
    }
}
