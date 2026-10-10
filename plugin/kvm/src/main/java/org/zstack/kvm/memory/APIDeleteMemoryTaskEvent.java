package org.zstack.kvm.memory;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import java.util.ArrayList;
import java.util.List;

@RestResponse(fieldsTo = {"taskUuid", "deleted", "scope", "resourceUuid", "hostUuids"})
public class APIDeleteMemoryTaskEvent extends APIEvent {
    private String taskUuid;
    private boolean deleted;
    private String scope;
    private String resourceUuid;
    private List<String> hostUuids = new ArrayList<>();

    public APIDeleteMemoryTaskEvent() { }
    public APIDeleteMemoryTaskEvent(String apiId) { super(apiId); }

    public String getTaskUuid() { return taskUuid; }
    public void setTaskUuid(String value) { taskUuid = value; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean value) { deleted = value; }
    public String getScope() { return scope; }
    public void setScope(String value) { scope = value; }
    public String getResourceUuid() { return resourceUuid; }
    public void setResourceUuid(String value) { resourceUuid = value; }
    public List<String> getHostUuids() { return hostUuids; }
    public void setHostUuids(List<String> value) { hostUuids = value; }

    public static APIDeleteMemoryTaskEvent __example__() {
        APIDeleteMemoryTaskEvent event = new APIDeleteMemoryTaskEvent();
        event.setTaskUuid("1234567890abcdef1234567890abcdef");
        event.setDeleted(true);
        event.setScope("Host");
        event.setResourceUuid("1234567890abcdef1234567890abcdef");
        event.setHostUuids(java.util.Collections.singletonList("1234567890abcdef1234567890abcdef"));
        return event;
    }
}
