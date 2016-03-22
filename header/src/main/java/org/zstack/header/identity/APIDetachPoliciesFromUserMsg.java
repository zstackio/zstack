package org.zstack.header.identity;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

import java.util.List;

/**
 * Created by xing5 on 2016/3/14.
 */
@Action(category = AccountConstant.ACTION_CATEGORY, accountOnly = true)
public class APIDetachPoliciesFromUserMsg extends APIMessage implements AccountMessage {
    @APIParam(resourceType = PolicyVO.class, checkAccount = true, operationTarget = true)
    private List<String> policyUuids;
    @APIParam(resourceType = UserVO.class, checkAccount = true, operationTarget = true)
    private String userUuid;

    public List<String> getPolicyUuids() {
        return policyUuids;
    }

    public void setPolicyUuids(List<String> policyUuids) {
        this.policyUuids = policyUuids;
    }

    public String getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }

    @Override
    public String getAccountUuid() {
        return getSession().getAccountUuid();
    }
}
