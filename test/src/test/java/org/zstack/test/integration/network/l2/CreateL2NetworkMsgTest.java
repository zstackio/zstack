package org.zstack.test.integration.network.l2;

import org.junit.Assert;
import org.junit.Test;
import org.zstack.header.network.l2.APICreateL2NetworkMsg;
import org.zstack.header.network.l2.APICreateL2NoVlanNetworkMsg;
import org.zstack.header.network.l2.CreateL2NetworkMsg;
import org.zstack.header.network.l2.ExternalNetworkRef;
import org.zstack.header.network.l2.NetworkCreateContext;
import org.zstack.header.network.l2.NetworkOperationOrigin;
import org.zstack.header.network.l3.CreateL3NetworkMsg;
import org.zstack.header.network.l3.AddIpRangeMsg;
import org.zstack.header.network.l3.DeleteProjectedIpRangeMsg;
import org.zstack.header.network.l3.L3NetworkMessage;
import org.zstack.header.network.l3.UpdateProjectedDnsMsg;
import org.zstack.header.network.l3.UpdateProjectedIpRangeMsg;
import org.zstack.network.l3.AttachNetworkServiceToL3Msg;
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

    @Test
    public void internalIpRangeCreateUsesInheritedSystemTags() {
        AddIpRangeMsg msg = new AddIpRangeMsg();
        msg.setSystemTags(Collections.singletonList("projection::zns"));

        String json = JSONObjectUtil.toJsonString(msg);

        Assert.assertEquals(1, json.split("\\\"systemTags\\\"", -1).length - 1);
        Assert.assertEquals("projection::zns", msg.getSystemTags().get(0));
    }

    @Test
    public void internalDnsProjectionCarriesLocalOnlyContext() {
        UpdateProjectedDnsMsg msg = new UpdateProjectedDnsMsg();
        msg.setL3NetworkUuid("l3-uuid");
        msg.setDns(Collections.singletonList("192.0.2.53"));
        msg.setContext(NetworkCreateContext.projection(NetworkOperationOrigin.ZNS_REFRESH,
                new ExternalNetworkRef("dhcp-config-uuid", "account-uuid")));

        Assert.assertTrue(msg.getContext().isProjection());
        Assert.assertEquals("192.0.2.53", msg.getDns().get(0));
    }

    @Test
    public void internalIpRangeRefreshCarriesLocalOnlyContext() {
        NetworkCreateContext context = NetworkCreateContext.projection(NetworkOperationOrigin.ZNS_REFRESH,
                new ExternalNetworkRef("range-uuid", "account-uuid"));
        UpdateProjectedIpRangeMsg update = new UpdateProjectedIpRangeMsg();
        update.setContext(context);
        DeleteProjectedIpRangeMsg delete = new DeleteProjectedIpRangeMsg();
        delete.setContext(context);

        Assert.assertTrue(update.getContext().isProjection());
        Assert.assertTrue(delete.getContext().isProjection());
        Assert.assertTrue(update instanceof L3NetworkMessage);
        Assert.assertTrue(delete instanceof L3NetworkMessage);
    }

    @Test
    public void internalNetworkServiceAttachCarriesLocalOnlyContext() {
        AttachNetworkServiceToL3Msg msg = new AttachNetworkServiceToL3Msg();
        msg.setContext(NetworkCreateContext.projection(NetworkOperationOrigin.ZNS_PROJECTION,
                new ExternalNetworkRef("dhcp-service-uuid", "account-uuid")));

        Assert.assertTrue(msg.getContext().isProjection());
    }
}
