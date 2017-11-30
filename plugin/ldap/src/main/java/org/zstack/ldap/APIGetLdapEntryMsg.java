package org.zstack.ldap;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by lining on 2017/11/03.
 */
@RestRequest(
        path = "/ldap/entry",
        method = HttpMethod.GET,
        responseClass = APIGetLdapEntryReply.class
)
public class APIGetLdapEntryMsg extends APISyncCallMessage {

    @APIParam
    private String ldapFilter;

    public String getLdapFilter() {
        return ldapFilter;
    }

    public void setLdapFilter(String ldapFilter) {
        this.ldapFilter = ldapFilter;
    }

    public static APIGetLdapEntryMsg __example__() {
        APIGetLdapEntryMsg msg = new APIGetLdapEntryMsg();
        msg.setLdapFilter("(cn=user_xxx)");

        return msg;
    }

}
