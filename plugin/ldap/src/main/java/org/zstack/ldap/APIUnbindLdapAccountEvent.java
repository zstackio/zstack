package org.zstack.ldap;

import org.zstack.header.message.APIEvent;

public class APIUnbindLdapAccountEvent extends APIEvent {
    public APIUnbindLdapAccountEvent(String apiId) {
        super(apiId);
    }

    public APIUnbindLdapAccountEvent() {
        super(null);
    }
}
