package org.zstack.test.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;
import org.zstack.header.network.NetworkConfigChange;
import org.zstack.utils.gson.JSONObjectUtil;

import static org.junit.Assert.*;

public class TestIpRangeRemovalSnapshot {
    @Test
    public void queuedRemovalPreservesOriginalBoundaries() {
        assertSnapshotRoundTrip(false, 4, "192.168.10.10", "192.168.10.100", null);
    }

    @Test
    public void lastRangeRemovalPreservesOriginalIpv6BoundariesAndMode() {
        assertSnapshotRoundTrip(true, 6, "2001:db8::10", "2001:db8::100", "Stateful-DHCP");
    }

    @Test
    public void legacyRemovalWithoutSnapshotRemainsReadable() {
        String json = removalJson(false, 4);
        NetworkConfigChange restored = JSONObjectUtil.toObject(json, NetworkConfigChange.class);
        assertEquals("old-range", restored.getIpRangeConfiguration().getRemovedRangeUuid());
        assertEquals(NetworkConfigChange.CollectionChangeOperation.REMOVE,
                restored.getIpRangeConfiguration().getOperation());
    }

    private void assertSnapshotRoundTrip(boolean lastRange, int family,
                                         String start, String end, String addressMode) {
        JsonObject input = new JsonParser().parse(removalJson(lastRange, family)).getAsJsonObject();
        JsonObject snapshot = new JsonObject();
        snapshot.addProperty("rangeUuid", "old-range");
        snapshot.addProperty("startIp", start);
        snapshot.addProperty("endIp", end);
        if (addressMode != null) {
            snapshot.addProperty("addressMode", addressMode);
        }
        input.getAsJsonObject("ipRangeConfiguration").add("removedRange", snapshot);

        NetworkConfigChange restored = JSONObjectUtil.toObject(input.toString(), NetworkConfigChange.class);
        JsonObject output = new JsonParser().parse(JSONObjectUtil.toJsonString(restored))
                .getAsJsonObject().getAsJsonObject("ipRangeConfiguration");
        assertTrue("queued range deletion lost its original start/end snapshot", output.has("removedRange"));
        assertEquals(snapshot, output.getAsJsonObject("removedRange"));
        assertEquals(lastRange, restored.getIpRangeConfiguration().isDelete());
    }

    private String removalJson(boolean lastRange, int family) {
        return "{\"kind\":\"IP_RANGE_CONFIGURATION\",\"l2Uuid\":\"l2\",\"origin\":\"API\","
                + "\"operationUuid\":\"operation\",\"ipRangeConfiguration\":{"
                + "\"l3Uuid\":\"l3\",\"ipVersion\":" + family + ",\"ranges\":[],"
                + "\"delete\":" + lastRange + ",\"operation\":\"REMOVE\","
                + "\"changedRanges\":[],\"removedRangeUuid\":\"old-range\"}}";
    }
}
