package org.zstack.test.tag;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.tag.SystemTagLifeCycleListener;
import org.zstack.header.tag.TagDefinition;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.header.zone.ZoneVO;
import org.zstack.tag.SystemTag;
import org.zstack.tag.SystemTagCreator;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

/**
 * test lifecycle listener
 */
public class TestSystemTag3 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    boolean deleteCalled = false;
    boolean createCalled = false;

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
        TestSystemTags.big.installLifeCycleListener(new SystemTagLifeCycleListener() {
            @Override
            public void tagCreated(SystemTagInventory tag) {
                if (tag.getTag().equals(TestSystemTags.big.getTagFormat())) {
                    createCalled = true;
                }
            }

            @Override
            public void tagDeleted(SystemTagInventory tag) {
                if (tag.getTag().equals(TestSystemTags.big.getTagFormat())) {
                    deleteCalled = true;
                }
            }

            @Override
            public void tagUpdated(SystemTagInventory old, SystemTagInventory newTag) {
            }
        });

        ZoneInventory zone1 = deployer.zones.get("Zone1");
        SystemTagCreator creator = TestSystemTags.big.newSystemTagCreator(zone1.getUuid());
        SystemTagInventory inv = creator.create();
        Assert.assertTrue(createCalled);
        api.deleteTag(inv.getUuid());
        createCalled = false;
        creator = TestSystemTags.big.newSystemTagCreator(zone1.getUuid());
        creator.inherent = true;
        creator.create();
        Assert.assertTrue(createCalled);
    }
}
