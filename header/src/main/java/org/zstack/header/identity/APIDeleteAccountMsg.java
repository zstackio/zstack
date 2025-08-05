package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.Constants;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 7/15/2015.
 */
@Action(category = AccountConstant.ACTION_CATEGORY, accountOnly = true)
@RestRequest(
        path = "/accounts/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteAccountEvent.class,
        morphTransform = Constants.MORPH_TRANSFORM_IAM1
)
public class APIDeleteAccountMsg extends APIDeleteMessage implements AccountMessage {
    @APIParam(resourceType = AccountVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getAccountUuid() {
        return uuid;
    }
 
    public static APIDeleteAccountMsg __example__() {
        APIDeleteAccountMsg msg = new APIDeleteAccountMsg();
        msg.setUuid(uuid());

        return msg;
    }
}
