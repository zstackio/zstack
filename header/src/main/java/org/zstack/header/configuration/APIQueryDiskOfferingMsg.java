package org.zstack.header.configuration;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

@AutoQuery(replyClass = APIQueryDiskOfferingReply.class, inventoryClass = DiskOfferingInventory.class)
@Action(category = ConfigurationConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/disk-offerings",
        optionalPaths = "/disk-offerings/{uuid}",
        method = HttpMethod.GET,
        responseClass = APIQueryDiskOfferingReply.class
)
public class APIQueryDiskOfferingMsg extends APIQueryMessage {

}
