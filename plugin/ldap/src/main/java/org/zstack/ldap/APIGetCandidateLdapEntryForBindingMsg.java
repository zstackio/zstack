package org.zstack.ldap;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by lining on 2017/12/03.
 */
@RestRequest(
        path = "/ldap/entries/candidates",
        method = HttpMethod.GET,
        responseClass = APIGetLdapEntryReply.class
)
public class APIGetCandidateLdapEntryForBindingMsg extends APISyncCallMessage {

    @APIParam
    private String ldapFilter;

    @APIParam(required = false, numberRange = {1, Integer.MAX_VALUE})
    private Integer limit = 2500;

    public String getLdapFilter() {
        return ldapFilter;
    }

    public void setLdapFilter(String ldapFilter) {
        this.ldapFilter = ldapFilter;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public static APIGetCandidateLdapEntryForBindingMsg __example__() {
        APIGetCandidateLdapEntryForBindingMsg msg = new APIGetCandidateLdapEntryForBindingMsg();
        msg.setLdapFilter("(cn=user_xxx)");

        return msg;
    }

}
