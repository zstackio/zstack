package org.zstack.header.configuration;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

@AutoQuery(replyClass = APIQueryInstanceOfferingReply.class, inventoryClass = InstanceOfferingInventory.class)
@Action(category = ConfigurationConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/instance-offerings",
        optionalPaths = "/instance-offerings/{uuid}",
        responseClass = APIQueryInstanceOfferingReply.class,
        method = HttpMethod.GET
)
public class APIQueryInstanceOfferingMsg extends APIQueryMessage {

}
