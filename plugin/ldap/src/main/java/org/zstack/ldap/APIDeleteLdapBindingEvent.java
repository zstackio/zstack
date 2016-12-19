package org.zstack.ldap;

import org.zstack.header.message.APIEvent;

public class APIDeleteLdapBindingEvent extends APIEvent {
    public APIDeleteLdapBindingEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteLdapBindingEvent() {
        super(null);
    }
}
