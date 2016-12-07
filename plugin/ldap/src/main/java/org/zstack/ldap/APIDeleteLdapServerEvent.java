package org.zstack.ldap;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteLdapServerEvent extends APIEvent {
    public APIDeleteLdapServerEvent() {
    }

    public APIDeleteLdapServerEvent(String apiId) {
        super(apiId);
    }
}
