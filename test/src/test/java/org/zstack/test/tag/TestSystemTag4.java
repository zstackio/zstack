package org.zstack.test.tag;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.tag.TagDefinition;
import org.zstack.header.zone.ZoneVO;
import org.zstack.tag.SystemTag;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;


/**
 */
public class TestSystemTag4 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

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
        thrown.expect(ApiSenderException.class);
        api.createSystemTag(randomUUID, "big", ZoneVO.class);

    }
}
