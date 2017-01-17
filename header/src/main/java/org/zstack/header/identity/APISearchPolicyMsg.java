package org.zstack.header.identity;

import org.zstack.header.search.APISearchMessage;

@NeedRoles(roles = {IdentityRoles.SEARCH_POLICY_ROLE})
public class APISearchPolicyMsg extends APISearchMessage implements AccountMessage {
    @Override
    public String getAccountUuid() {
        return this.getSession().getAccountUuid();
    }
 
    public static APISearchPolicyMsg __example__() {
        APISearchPolicyMsg msg = new APISearchPolicyMsg();


        return msg;
    }

}
