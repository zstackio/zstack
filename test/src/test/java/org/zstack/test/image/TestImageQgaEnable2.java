package org.zstack.test.image;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.APIGetImageQgaEnableReply;
import org.zstack.header.image.APISetImageQgaDisableEvent;
import org.zstack.header.image.APISetImageQgaEnableEvent;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.image.ImageSystemTags;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.ArrayList;

/**
 * Created by mingjian.deng on 17/1/5.
 */
public class TestImageQgaEnable2 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/image/TestQueryImage.xml");
        deployer.addSpringConfig("mevocoRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        ImageInventory image = deployer.images.get("TestImage");
        SessionInventory session = api.loginByAccount("test", "password");

        APIGetImageQgaEnableReply reply = api.getEnableImageQga(image.getUuid());
        Assert.assertTrue(reply.isSuccess());

        ArrayList resourceUuids = new ArrayList<String >();
        resourceUuids.add(image.getUuid());
        ArrayList accountUuids = new ArrayList<String >();
        accountUuids.add(session.getAccountUuid());
        api.shareResource(resourceUuids, accountUuids ,false);


        thrown.expect(ApiSenderException.class);
        APISetImageQgaEnableEvent evt = api.enableImageQga(session, image.getUuid());

        reply = api.getEnableImageQga(image.getUuid());
        Assert.assertTrue(reply.isSuccess());

        thrown.expect(ApiSenderException.class);
        APISetImageQgaDisableEvent disableEvent = api.disableImageQga(session, image.getUuid());
        Assert.assertFalse(disableEvent.isSuccess());

    }

}
