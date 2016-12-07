package org.zstack.header.allocator;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 8:46 PM
 * To change this template use File | Settings | File Templates.
 */
@RestResponse(fieldsTo = "strategies=hostAllocatorStrategies")
public class APIGetHostAllocatorStrategiesReply extends APIReply {
    private List<String> hostAllocatorStrategies;

    public List<String> getHostAllocatorStrategies() {
        return hostAllocatorStrategies;
    }

    public void setHostAllocatorStrategies(List<String> hostAllocatorStrategies) {
        this.hostAllocatorStrategies = hostAllocatorStrategies;
    }
}
