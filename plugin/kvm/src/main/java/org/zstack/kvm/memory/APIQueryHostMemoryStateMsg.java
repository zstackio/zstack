package org.zstack.kvm.memory;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryHostMemoryStateReply.class, inventoryClass = HostMemoryStateInventory.class)
@Action(category = "memoryOptimization", adminOnly = true)
@RestRequest(path = "/memory-optimization/host-states", responseClass = APIQueryHostMemoryStateReply.class,
        method = HttpMethod.GET)
public class APIQueryHostMemoryStateMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("hostUuid=1234567890abcdef1234567890abcdef", "featureState=Active");
    }
}
