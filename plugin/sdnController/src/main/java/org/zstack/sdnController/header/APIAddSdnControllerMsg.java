package org.zstack.sdnController.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.*;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

@TagResourceType(SdnControllerVO.class)
@Action(category = SdnControllerConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/sdn-controllers",
        method = HttpMethod.POST,
        responseClass = APIAddSdnControllerEvent.class,
        parameterName = "params"
)
public class APIAddSdnControllerMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 255)
    private String vendorType;

    @APIParam(maxLength = 255)
    private String name;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(maxLength = 255)
    private String ip;

    @APIParam(maxLength = 255)
    private String userName;

    @APIParam(maxLength = 255)
    @NoLogging
    private String password;

    public String getVendorType() {
        return vendorType;
    }

    public void setVendorType(String vendorType) {
        this.vendorType = vendorType;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

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

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APIAddSdnControllerEvent)rsp).getInventory().getUuid() : "", SdnControllerVO.class);
    }

    public static APIAddSdnControllerMsg __example__() {
        APIAddSdnControllerMsg msg = new APIAddSdnControllerMsg();

        msg.setVendorType("vendor");
        msg.setName("sdn-1");
        msg.setDescription("sdn controller from vendor");
        msg.setIp("192.168.1.1");
        msg.setUserName("admin");
        msg.setPassword("password");

        return msg;
    }
}
