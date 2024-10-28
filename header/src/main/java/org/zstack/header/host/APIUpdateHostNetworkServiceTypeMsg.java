package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by boce.wang on 10/25/2024.
 */
@RestRequest(
        path = "/hosts/service-types/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateHostNetworkServiceTypeEvent.class

)
public class APIUpdateHostNetworkServiceTypeMsg extends APIMessage {

    @APIParam(resourceType = HostNetworkLabelVO.class)
    private String uuid;

    @APIParam(maxLength = 128, nonempty = true, noTrim = true)
    private String serviceType;

    @APIParam(required = false, nonempty = true)
    private boolean system = false;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

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

    public static APIUpdateHostNetworkServiceTypeMsg __example__() {
        APIUpdateHostNetworkServiceTypeMsg msg = new APIUpdateHostNetworkServiceTypeMsg();
        msg.setUuid(uuid());
        msg.setServiceType("ManagementNetwork");
        msg.setSystem(true);
        return msg;
    }
}
