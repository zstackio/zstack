package org.zstack.sdnController.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.network.sdncontroller.SdnControllerConstant;
import org.zstack.header.network.sdncontroller.SdnControllerMessage;
import org.zstack.header.network.sdncontroller.SdnControllerVO;
import org.zstack.header.rest.RestRequest;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * API message for changing SDN controller configuration
 */
@Action(category = SdnControllerConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/sdn-controllers/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIChangeSdnControllerEvent.class,
        isAction = true
)
@DefaultTimeout(timeunit = TimeUnit.MINUTES, value = 30)
public class APIChangeSdnControllerMsg extends APIMessage implements SdnControllerMessage {
    
    @APIParam(resourceType = SdnControllerVO.class, checkAccount = true, operationTarget = true)
    private String uuid;

    @APIParam(required = false, maxLength = 255)
    private String userName;

    @APIParam(required = false, maxLength = 255)
    @NoLogging
    private String password;

    @APIParam(required = false, maxLength = 255)
    private List<String> vlanRanges;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getSdnControllerUuid() {
        return uuid;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public List<String> getVlanRanges() {
        return vlanRanges;
    }

    public void setVlanRanges(List<String> vlanRanges) {
        this.vlanRanges = vlanRanges;
    }

    public static APIChangeSdnControllerMsg __example__() {
        APIChangeSdnControllerMsg msg = new APIChangeSdnControllerMsg();
        msg.setUuid(uuid());
        msg.setUserName("sdnuser");
        msg.setPassword("newpassword");
        return msg;
    }
}
