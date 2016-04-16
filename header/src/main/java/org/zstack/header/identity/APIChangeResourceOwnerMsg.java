package org.zstack.header.identity;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

/**
 * Created by xing5 on 2016/4/16.
 */
public class APIChangeResourceOwnerMsg extends APIMessage {
    @APIParam(resourceType = AccountVO.class)
    private String accountUuid;
    @APIParam
    private String resourceUuid;

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }
}
