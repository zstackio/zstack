package org.zstack.test.core.rest;

import org.junit.Assert;
import org.junit.Test;
import org.zstack.header.rest.SyncHttpResponse;

public class TestSyncHttpResponse {
    @Test
    public void preservesStatusAndBody() {
        SyncHttpResponse response = new SyncHttpResponse(409, "conflict");
        Assert.assertEquals(409, response.getStatus());
        Assert.assertEquals("conflict", response.getBody());
    }
}
