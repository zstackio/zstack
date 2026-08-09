package org.zstack.test.integration.network.l2;

import org.junit.Assert;
import org.junit.Test;
import org.zstack.header.network.l2.APICreateL2NetworkMsg;
import org.zstack.header.network.l2.APICreateL2NoVlanNetworkMsg;
import org.zstack.header.network.l2.CreateL2NetworkMsg;

public class CreateL2NetworkMsgTest {
    @Test
    public void internalCreateCarriesProviderSubtypeMessage() {
        CreateL2NetworkMsg msg = new CreateL2NetworkMsg();
        APICreateL2NetworkMsg subtype = new APICreateL2NoVlanNetworkMsg();

        msg.setSubtypeMessage(subtype);

        Assert.assertSame(subtype, msg.getSubtypeMessage());
    }
}
