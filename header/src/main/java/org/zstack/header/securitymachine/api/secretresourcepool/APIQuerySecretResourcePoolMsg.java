package org.zstack.header.securitymachine.api.secretresourcepool;

import org.springframework.http.HttpMethod;
import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolConstant;
import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolInventory;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

/**
 * Created by LiangHanYu on 2021/11/3 17:05
 */
@Action(category = SecretResourcePoolConstant.CATEGORY, names = {"read"})
@AutoQuery(replyClass = APIQuerySecretResourcePoolReply.class, inventoryClass = SecretResourcePoolInventory.class)
@RestRequest(
        path = "/secret-resource-pools",
        optionalPaths = {"/secret-resource-pool/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQuerySecretResourcePoolReply.class
)
public class APIQuerySecretResourcePoolMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return Collections.singletonList("uuid=" + uuid());
    }
}
