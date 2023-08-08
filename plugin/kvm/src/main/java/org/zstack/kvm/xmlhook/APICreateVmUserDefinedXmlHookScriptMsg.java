package org.zstack.kvm.xmlhook;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.*;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

import java.util.concurrent.TimeUnit;

@RestRequest(
        path = "/vm-instances/xml-hook-script",
        method = HttpMethod.POST,
        responseClass = APICreateVmUserDefinedXmlHookScriptEvent.class,
        parameterName = "params"
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 12)
public class APICreateVmUserDefinedXmlHookScriptMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 255)
    private String name;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam
    private String hookScript;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHookScript() {
        return hookScript;
    }

    public void setHookScript(String hookScript) {
        this.hookScript = hookScript;
    }

    public static APICreateVmUserDefinedXmlHookScriptMsg __example__() {
        APICreateVmUserDefinedXmlHookScriptMsg msg = new APICreateVmUserDefinedXmlHookScriptMsg();
        msg.setName("hook");
        msg.setDescription("xml hook");
        msg.setHookScript("base64");
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(((APICreateVmUserDefinedXmlHookScriptMsg) msg).getName(), XmlHookVO.class);
    }
}
