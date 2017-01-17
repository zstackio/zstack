package org.zstack.header.identity;

import org.zstack.header.search.APISearchMessage;

@NeedRoles(roles = {IdentityRoles.SEARCH_USER_GROUP_ROLE})
public class APISearchUserGroupMsg extends APISearchMessage implements AccountMessage {
    @Override
    public String getAccountUuid() {
        return this.getSession().getAccountUuid();
    }
 
    public static APISearchUserGroupMsg __example__() {
        APISearchUserGroupMsg msg = new APISearchUserGroupMsg();


        return msg;
    }

}
