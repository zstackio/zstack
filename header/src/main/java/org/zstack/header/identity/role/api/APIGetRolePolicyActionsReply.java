package org.zstack.header.identity.role.api;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by Wenhao.Zhang on 2024/08/30
 */
@RestResponse(allTo = "inventories")
public class APIGetRolePolicyActionsReply extends APIReply {
    @NoLogging
    private List<String> inventories;

    public List<String> getInventories() {
        return inventories;
    }

    public void setInventories(List<String> inventories) {
        this.inventories = inventories;
    }

    public static APIGetRolePolicyActionsReply __example__() {
        APIGetRolePolicyActionsReply reply = new APIGetRolePolicyActionsReply();
        reply.setInventories(list(
            ".header.identity.APIChangeResourceOwnerMsg",
            ".header.identity.APICheckResourcePermissionMsg",
            ".header.identity.APICreateAccountMsg",
            ".header.identity.APIDeleteAccountMsg",
            ".header.identity.APIGetAccountQuotaUsageMsg",
            ".header.identity.APIGetActionsMsg",
            ".header.identity.APIGetResourceAccountMsg",
            ".header.identity.APILogInByAccountMsg",
            ".header.identity.APILogOutMsg",
            ".header.identity.APIQueryAccountMsg",
            ".header.identity.APIQueryAccountResourceRefMsg",
            ".header.identity.APIQueryQuotaMsg",
            ".header.identity.APIRenewSessionMsg",
            ".header.identity.APIRevokeResourceSharingMsg",
            ".header.identity.APIShareResourceMsg",
            ".header.identity.APIUpdateAccountMsg",
            ".header.identity.APIUpdateQuotaMsg",
            ".header.identity.APIValidateSessionMsg"
        ));
        return reply;
    }
}
