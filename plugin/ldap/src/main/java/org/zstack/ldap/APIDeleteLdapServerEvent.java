package org.zstack.ldap;

import org.zstack.header.message.APIEvent;

public class APIDeleteLdapServerEvent extends APIEvent {
    public APIDeleteLdapServerEvent() {
    }

    public APIDeleteLdapServerEvent(String apiId) {
        super(apiId);
    }
}
