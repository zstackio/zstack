package org.zstack.test.integration.networkservice.provider.zns;

import org.junit.Test;
import org.zstack.header.network.NetworkConfigMutation;

import java.util.HashMap;
import java.util.Map;

public class NetworkConfigMutationTest {
    @Test
    public void fieldsAreDefensivelyCopied() {
        Map<String, Object> fields = new HashMap<>();
        fields.put("mtu", 1500);
        NetworkConfigMutation mutation = new NetworkConfigMutation("r", "op", 1, "APPLY_LOCAL", fields);
        fields.put("mtu", 9000);
        if (!Integer.valueOf(1500).equals(mutation.getFields().get("mtu"))) {
            throw new AssertionError("mutation fields changed after construction");
        }
    }
}
