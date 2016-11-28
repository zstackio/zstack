package org.zstack.test.identity;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.apimediator.ApiMediatorConstant;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.identity.UserVO;
import org.zstack.header.identity.UserVO_;
import org.zstack.test.Api;
import org.zstack.test.ApiSender;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

public class TestMessageHas2RolePolicyAllow {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/identity/TestPolicyMessageHas2RoleAllow.xml");
        deployer.addSpringConfig("FakeAuthorizationServiceForRoleTest.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException {
        SimpleQuery<UserVO> query = dbf.createQuery(UserVO.class);
        query.add(UserVO_.name, Op.EQ, "TestUser");
        UserVO user = query.find();
        SessionInventory session = api.loginByUser(user.getName(), user.getPassword(), user.getAccountUuid());
        FakePolicyAllowHas2RoleMsg msg = new FakePolicyAllowHas2RoleMsg();
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(session);
        ApiSender sender = new ApiSender();
        sender.send(msg, FakeApiEvent.class);
    }

}
