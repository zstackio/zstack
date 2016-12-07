package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.List;

/**
 * Created by frank on 7/13/2015.
 */
@Action(category = AccountConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/accounts/resources/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIRevokeResourceSharingEvent.class
)
public class APIRevokeResourceSharingMsg extends APIMessage implements AccountMessage {
    @APIParam(nonempty = true)
    private List<String> resourceUuids;
    private boolean toPublic;
    @APIParam(resourceType = AccountVO.class, required = false)
    private List<String> accountUuids;
    private boolean all;

    public boolean isAll() {
        return all;
    }

    public void setAll(boolean all) {
        this.all = all;
    }

    public List<String> getResourceUuids() {
        return resourceUuids;
    }

    public void setResourceUuids(List<String> resourceUuids) {
        this.resourceUuids = resourceUuids;
    }

    public boolean isToPublic() {
        return toPublic;
    }

    public void setToPublic(boolean toPublic) {
        this.toPublic = toPublic;
    }

    public List<String> getAccountUuids() {
        return accountUuids;
    }

    public void setAccountUuids(List<String> accountUuids) {
        this.accountUuids = accountUuids;
    }

    @Override
    public String getAccountUuid() {
        return getSession().getAccountUuid();
    }
}
