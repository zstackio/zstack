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
        responseClass = APIShareResourceEvent.class,
        isAction = true
)
public class APIShareResourceMsg extends APIMessage implements AccountMessage {
    @APIParam(nonempty = true, checkAccount = true, operationTarget = true)
    private List<String> resourceUuids;
    @APIParam(resourceType = AccountVO.class, required = false)
    private List<String> accountUuids;
    private boolean toPublic;

    public List<String> getResourceUuids() {
        return resourceUuids;
    }

    public void setResourceUuids(List<String> resourceUuids) {
        this.resourceUuids = resourceUuids;
    }

    public List<String> getAccountUuids() {
        return accountUuids;
    }

    public void setAccountUuids(List<String> accountUuids) {
        this.accountUuids = accountUuids;
    }

    public boolean isToPublic() {
        return toPublic;
    }

    public void setToPublic(boolean toPublic) {
        this.toPublic = toPublic;
    }

    @Override
    public String getAccountUuid() {
        return getSession().getAccountUuid();
    }
}
