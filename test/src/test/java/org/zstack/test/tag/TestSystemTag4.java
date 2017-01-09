package org.zstack.test.tag;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.query.QueryOp;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.header.zone.APIQueryZoneMsg;
import org.zstack.header.zone.APIQueryZoneReply;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.header.zone.ZoneVO;
import org.zstack.tag.SystemTag;
import org.zstack.tag.TagSubQueryExtension;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.List;

/**
 */
public class TestSystemTag4 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @TagDefinition
    public static class TestSystemTags {
        public static SystemTag big = new SystemTag("big", ZoneVO.class);
    }

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/tag/TestUserTag.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException {
        String randomUUID = Platform.getUuid();
        try {
            api.createSystemTag(randomUUID, "big", ZoneVO.class);
        } catch (ApiSenderException e) {
            Assert.assertEquals(e instanceof ApiSenderException, true);
        } catch (Throwable t) {
            Assert.assertEquals(t.toString(), "Unknown error");
        }
    }
}
