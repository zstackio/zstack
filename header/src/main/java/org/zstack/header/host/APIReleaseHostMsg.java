package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/hosts/release",
        method = HttpMethod.POST,
        responseClass = APIReleaseHostEvent.class,
        parameterName = "params"
)
public class APIReleaseHostMsg extends APISyncCallMessage {
    @APIParam(required = true, nonempty = true)
    private String productUuid;

    public String getProductUuid() {
        return productUuid;
    }

    public void setProductUuid(String productUuid) {
        this.productUuid = productUuid;
    }

    public static APIReleaseHostMsg __example__() {
        APIReleaseHostMsg msg = new APIReleaseHostMsg();
        msg.setProductUuid("efc58407-0403-4c5e-b9af-03d19b2e855f");
        return msg;
    }
}