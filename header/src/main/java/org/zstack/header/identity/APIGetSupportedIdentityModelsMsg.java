package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@Action(category = AccountConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/identity-models",
        method = "GET",
        responseClass = APIGetSupportedIdentityModelsReply.class
)
@SuppressCredentialCheck
public class APIGetSupportedIdentityModelsMsg extends APISyncCallMessage {
}
