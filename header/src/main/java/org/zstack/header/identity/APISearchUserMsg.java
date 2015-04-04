package org.zstack.header.identity;

import org.zstack.header.search.APISearchMessage;

@NeedRoles(roles = {IdentityRoles.SEARCH_USER_ROLE})
public class APISearchUserMsg extends APISearchMessage implements AccountMessage {
    @Override
    public String getAccountUuid() {
        return this.getSession().getAccountUuid();
    }
}
