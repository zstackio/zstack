package org.zstack.test.image;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.image.APIGetImageQgaReply;
import org.zstack.header.image.APISetImageQgaEvent;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.image.ImageSystemTags;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

/**
 * Created by mingjian.deng on 17/1/5.
 */
public class TestImageQgaEnable {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

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

        APIGetImageQgaReply reply = api.getEnableImageQga(image.getUuid());
        Assert.assertTrue(reply.isSuccess());
        Assert.assertFalse(reply.isEnable());

        APISetImageQgaEvent evt = api.enableImageQga(image.getUuid());
        Assert.assertTrue(evt.isSuccess());
        String tag = getResourceUuidTag(image.getUuid());
        Assert.assertEquals(ImageSystemTags.IMAGE_INJECT_QEMUGA.getTagFormat(), tag);

        reply = api.getEnableImageQga(image.getUuid());
        Assert.assertTrue(reply.isSuccess());
        Assert.assertTrue(reply.isEnable());

        APISetImageQgaEvent disableEvent = api.disableImageQga(image.getUuid());
        Assert.assertTrue(disableEvent.isSuccess());
        tag = getResourceUuidTag(image.getUuid());
        Assert.assertNull(tag);

        reply = api.getEnableImageQga(image.getUuid());
        Assert.assertTrue(reply.isSuccess());
        Assert.assertFalse(reply.isEnable());
    }

    String getResourceUuidTag(String resourceUuid) {
        SimpleQuery<SystemTagVO> pq = dbf.createQuery(SystemTagVO.class);
        pq.select(SystemTagVO_.tag);
        pq.add(SystemTagVO_.resourceUuid, SimpleQuery.Op.EQ, resourceUuid);
        String tag = pq.findValue();
        return tag;
    }
}
