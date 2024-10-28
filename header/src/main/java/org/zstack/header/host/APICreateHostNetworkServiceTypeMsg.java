package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

/**
 * Created by boce.wang on 10/24/2024.
 */
@TagResourceType(HostNetworkLabelVO.class)
@Action(category = HostConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/hosts/service-types",
        method = HttpMethod.POST,
        responseClass = APICreateHostNetworkServiceTypeEvent.class,
        parameterName = "params"
)
public class APICreateHostNetworkServiceTypeMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 128, nonempty = true, noTrim = true)
    private String serviceType;

    @APIParam(required = false, nonempty = true)
    private boolean system = false;

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    public static APICreateHostNetworkServiceTypeMsg __example__() {
        APICreateHostNetworkServiceTypeMsg msg = new APICreateHostNetworkServiceTypeMsg();
        msg.setServiceType("ManagementNetwork");
        msg.setSystem(true);
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        String resUuid = "";
        if (rsp.isSuccess()) {
            APICreateHostNetworkServiceTypeEvent evt = (APICreateHostNetworkServiceTypeEvent) rsp;
            resUuid = evt.getInventory().getUuid();
        }
        return new Result(resUuid, HostNetworkLabelVO.class);
    }
}
