package org.zstack.header.securitymachine.api.securitymachine;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"all"})
public class APIGetIntegrityEncryptValueEvent extends APIEvent {
    private String result;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public APIGetIntegrityEncryptValueEvent() {
    }

    public APIGetIntegrityEncryptValueEvent(String apiId) {
        super(apiId);
    }

    public static APIGetIntegrityEncryptValueEvent __example__() {
        APIGetIntegrityEncryptValueEvent ret = new APIGetIntegrityEncryptValueEvent();
        ret.result = uuid();
        return ret;
    }
}
