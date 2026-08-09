package org.zstack.test.integration.network.l2;

import org.junit.Assert;
import org.junit.Test;
import org.zstack.header.network.l2.APICreateL2NetworkMsg;
import org.zstack.header.network.l2.APICreateL2NoVlanNetworkMsg;
import org.zstack.header.network.l2.CreateL2NetworkMsg;
import org.zstack.header.network.l3.CreateL3NetworkMsg;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.Collections;

public class CreateL2NetworkMsgTest {
    @Test
    public void internalCreateCarriesProviderSubtypeMessage() {
        CreateL2NetworkMsg msg = new CreateL2NetworkMsg();
        APICreateL2NetworkMsg subtype = new APICreateL2NoVlanNetworkMsg();

        msg.setSubtypeMessage(subtype);

        Assert.assertSame(subtype, msg.getSubtypeMessage());
    }

    @Test
    public void internalCreateUsesInheritedSystemTags() {
        CreateL2NetworkMsg msg = new CreateL2NetworkMsg();
        msg.setSystemTags(Collections.singletonList("projection::zns"));

        String json = JSONObjectUtil.toJsonString(msg);

        Assert.assertEquals(1, json.split("\\\"systemTags\\\"", -1).length - 1);
        Assert.assertEquals("projection::zns", msg.getSystemTags().get(0));
    }

    @Test
    public void internalL3CreateUsesInheritedSystemTags() {
        CreateL3NetworkMsg msg = new CreateL3NetworkMsg();
        msg.setSystemTags(Collections.singletonList("projection::zns"));

        String json = JSONObjectUtil.toJsonString(msg);

        Assert.assertEquals(1, json.split("\\\"systemTags\\\"", -1).length - 1);
        Assert.assertEquals("projection::zns", msg.getSystemTags().get(0));
    }
}
