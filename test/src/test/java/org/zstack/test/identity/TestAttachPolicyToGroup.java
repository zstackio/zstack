package org.zstack.test.identity;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.identity.PolicyVO;
import org.zstack.header.identity.PolicyVO_;
import org.zstack.header.identity.UserGroupVO;
import org.zstack.header.identity.UserVO_;
import org.zstack.test.Api;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

public class TestAttachPolicyToGroup {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/identity/TestAttachPolicyToGroup.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() {
        SimpleQuery<UserGroupVO> q = dbf.createQuery(UserGroupVO.class);
        q.add(UserVO_.name, Op.EQ, "TestGroup1");
        UserGroupVO vo = q.find();
        Assert.assertTrue(!vo.getPolicies().isEmpty());
        SimpleQuery<PolicyVO> pq = dbf.createQuery(PolicyVO.class);
        pq.add(PolicyVO_.name, Op.EQ, "TestPolicy");
        PolicyVO pvo = pq.find();
        PolicyVO pvo1 = vo.getPolicies().iterator().next();
        Assert.assertEquals(pvo.getUuid(), pvo1.getUuid());
    }
}
