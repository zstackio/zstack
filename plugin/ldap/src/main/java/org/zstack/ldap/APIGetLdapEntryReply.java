package org.zstack.ldap;

import org.zstack.header.message.APIReply;
import org.zstack.header.message.NoJsonSchema;
import org.zstack.header.rest.RestResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lining on 2017/11/03.
 */
@RestResponse(allTo = "inventories")
public class APIGetLdapEntryReply extends APIReply {
    @NoJsonSchema
    private List inventories;

    public List getInventories() {
        return inventories;
    }

    public void setInventories(List inventories) {
        this.inventories = inventories;
    }
 
    public static APIGetLdapEntryReply __example__() {
        APIGetLdapEntryReply reply = new APIGetLdapEntryReply();
        reply.setInventories(new ArrayList());
        return reply;
    }

}
