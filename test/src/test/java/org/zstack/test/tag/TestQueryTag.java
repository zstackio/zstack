package org.zstack.test.tag;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.*;
import org.zstack.storage.primary.PrimaryStorageSystemTags;
import org.zstack.test.Api;
import org.zstack.test.BeanConstructor;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.search.QueryTestValidator;

/**
 */
public class TestQueryTag {
    ComponentLoader loader;
    DatabaseFacade dbf;
    Api api;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        con.addXml("PortalForUnitTest.xml").addXml("AccountManager.xml");
        loader = con.build();
        dbf = loader.getComponent(DatabaseFacade.class);
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() {
        SystemTagVO vo = new SystemTagVO();
        vo.setResourceUuid(Platform.getUuid());
        vo.setUuid(Platform.getUuid());
        vo.setTag(PrimaryStorageSystemTags.CAPABILITY_SNAPSHOT.getTagFormat());
        vo.setResourceType(PrimaryStorageVO.class.getSimpleName());
        vo = dbf.persistAndRefresh(vo);

        SystemTagInventory inv = SystemTagInventory.valueOf(vo);
        APIQuerySystemTagMsg msg = new APIQuerySystemTagMsg();
        QueryTestValidator.validateEQ(msg, api, APIQuerySystemTagReply.class, inv);

        UserTagVO uvo = new UserTagVO();
        uvo.setResourceUuid(Platform.getUuid());
        uvo.setUuid(Platform.getUuid());
        uvo.setTag("adfad");
        uvo.setResourceType(PrimaryStorageVO.class.getSimpleName());
        uvo = dbf.persistAndRefresh(uvo);
        UserTagInventory uinv = UserTagInventory.valueOf(uvo);

        APIQueryUserTagMsg umsg = new APIQueryUserTagMsg();
        QueryTestValidator.validateRandomEQConjunction(umsg, api, APIQueryUserTagReply.class, uinv, 3);
    }
}
