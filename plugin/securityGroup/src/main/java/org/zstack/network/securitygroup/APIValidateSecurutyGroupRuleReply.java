package org.zstack.network.securitygroup;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"all"})
public class APIValidateSecurutyGroupRuleReply extends APIReply {
    private boolean available;
    private String reason;

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public static APIValidateSecurutyGroupRuleReply __example__() {
        APIValidateSecurutyGroupRuleReply reply = new APIValidateSecurutyGroupRuleReply();
        reply.setAvailable(true);
        return reply;
    }
}
