package org.zstack.ldap.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by Wenhao.Zhang on 2024/06/04
 */
@RestResponse(fieldsTo = "all")
public class APISyncAccountsFromLdapServerEvent extends APIEvent {
    public APISyncAccountsFromLdapServerEvent(String apiId) {
        super(apiId);
    }

    public APISyncAccountsFromLdapServerEvent() {
        super(null);
    }
}
