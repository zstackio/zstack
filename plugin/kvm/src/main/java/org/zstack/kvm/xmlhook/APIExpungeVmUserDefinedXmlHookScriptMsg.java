package org.zstack.kvm.xmlhook;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/vm-instances/xml-hook-script",
        method = HttpMethod.DELETE,
        responseClass = APIExpungeVmUserDefinedXmlHookScriptEvent.class
)
public class APIExpungeVmUserDefinedXmlHookScriptMsg extends APIMessage implements XmlHookMessage {
    @APIParam(resourceType = XmlHookVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIExpungeVmUserDefinedXmlHookScriptMsg __example__() {
        APIExpungeVmUserDefinedXmlHookScriptMsg msg = new APIExpungeVmUserDefinedXmlHookScriptMsg();
        msg.setUuid(uuid());
        return msg;
    }

    @Override
    public String getXmlHookUuid() {
        return uuid;
    }
}
