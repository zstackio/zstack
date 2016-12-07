package org.zstack.header.allocator;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 8:46 PM
 * To change this template use File | Settings | File Templates.
 */
@RestRequest(
        path = "/hosts/allocators/strategies",
        method = HttpMethod.GET,
        parameterName = "null",
        responseClass = APIGetHostAllocatorStrategiesReply.class
)
public class APIGetHostAllocatorStrategiesMsg extends APISyncCallMessage {
}
