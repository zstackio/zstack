package org.zstack.header.vm;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * author:kaicai.hu
 * Date:2019/9/16
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetSpiceCertificatesReply extends APIReply {
    private String certificateStr;

    public String getCertificateStr() {
        return certificateStr;
    }

    public void setCertificateStr(String certificateStr) {
        this.certificateStr = certificateStr;
    }

    public static APIGetSpiceCertificatesReply __example__() {
        APIGetSpiceCertificatesReply reply = new APIGetSpiceCertificatesReply();
        reply.setCertificateStr("certificateStr");
        return reply;
    }
}
