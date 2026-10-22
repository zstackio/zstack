package org.zstack.network.securitygroup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

@RestResponse(fieldsTo = {"securityGroupUuid", "mode", "accountUuids"})
public class APISetSecurityGroupSharingEvent extends APIEvent {
    private String securityGroupUuid;
    private String mode;
    private List<String> accountUuids;

    public APISetSecurityGroupSharingEvent() {
        super(null);
    }

    public APISetSecurityGroupSharingEvent(String apiId) {
        super(apiId);
    }

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

    public static APISetSecurityGroupSharingEvent __example__() {
        APISetSecurityGroupSharingEvent event = new APISetSecurityGroupSharingEvent();
        event.setSecurityGroupUuid(uuid());
        event.setMode(SecurityGroupConstant.SHARING_MODE_SPECIFIED);
        event.setAccountUuids(Collections.singletonList(uuid()));
        return event;
    }
}
