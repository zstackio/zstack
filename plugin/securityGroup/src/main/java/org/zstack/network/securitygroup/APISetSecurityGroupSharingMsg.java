package org.zstack.network.securitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

@Action(category = SecurityGroupConstant.ACTION_CATEGORY)
@RestRequest(path = "/security-groups/{securityGroupUuid}/actions", method = HttpMethod.PUT,
        isAction = true, responseClass = APISetSecurityGroupSharingEvent.class)
public class APISetSecurityGroupSharingMsg extends APIMessage implements SecurityGroupMessage {
    @APIParam(resourceType = SecurityGroupVO.class, checkAccount = true, operationTarget = true)
    private String securityGroupUuid;

    @APIParam(validValues = {SecurityGroupConstant.SHARING_MODE_NONE, SecurityGroupConstant.SHARING_MODE_SPECIFIED, SecurityGroupConstant.SHARING_MODE_PUBLIC})
    private String mode;

    @APIParam(resourceType = AccountVO.class, required = false)
    private List<String> accountUuids;

    @Override
    public String getSecurityGroupUuid() {
        return securityGroupUuid;
    }

    public void setSecurityGroupUuid(String securityGroupUuid) {
        this.securityGroupUuid = securityGroupUuid;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public List<String> getAccountUuids() {
        return accountUuids;
    }

    public void setAccountUuids(List<String> accountUuids) {
        this.accountUuids = accountUuids;
    }

    public static APISetSecurityGroupSharingMsg __example__() {
        APISetSecurityGroupSharingMsg msg = new APISetSecurityGroupSharingMsg();
        msg.setSecurityGroupUuid(uuid());
        msg.setMode(SecurityGroupConstant.SHARING_MODE_SPECIFIED);
        msg.setAccountUuids(Collections.singletonList(uuid()));
        return msg;
    }
}
