package org.zstack.header.securitymachine.api.securitymachine;

import org.springframework.http.HttpMethod;
import org.zstack.header.securitymachine.SecurityMachineConstant;
import org.zstack.header.securitymachine.SecurityMachineInventory;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

/**
 * Created by LiangHanYu on 2021/11/3 17:27
 */
@Action(category = SecurityMachineConstant.CATEGORY, names = {"read"})
@AutoQuery(replyClass = APIQuerySecurityMachineReply.class, inventoryClass = SecurityMachineInventory.class)
@RestRequest(
        path = "/security-machines",
        optionalPaths = {"/security-machine/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQuerySecurityMachineReply.class
)
public class APIQuerySecurityMachineMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return Collections.singletonList("uuid=" + uuid());
    }
}