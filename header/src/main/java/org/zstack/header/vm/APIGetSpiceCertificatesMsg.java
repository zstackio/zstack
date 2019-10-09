package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@Action(category = VmInstanceConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/spice/certificates",
        method = HttpMethod.GET,
        responseClass = APIGetSpiceCertificatesReply.class
)
public class APIGetSpiceCertificatesMsg extends APISyncCallMessage {

    public static APIGetSpiceCertificatesMsg __example__() {
        APIGetSpiceCertificatesMsg msg = new APIGetSpiceCertificatesMsg();
        return msg;
    }
}
