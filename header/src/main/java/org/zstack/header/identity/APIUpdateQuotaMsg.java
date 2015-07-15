package org.zstack.header.identity;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

/**
 * Created by frank on 7/14/2015.
 */
public class APIUpdateQuotaMsg extends APIMessage implements AccountMessage {
    @APIParam(resourceType = AccountVO.class)
    private String identityUuid;
    @APIParam
    private String name;
    @APIParam
    private long value;

    @Override
    public String getAccountUuid() {
        return getSession().getAccountUuid();
    }

    public String getIdentityUuid() {
        return identityUuid;
    }

    public void setIdentityUuid(String identityUuid) {
        this.identityUuid = identityUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }
}
