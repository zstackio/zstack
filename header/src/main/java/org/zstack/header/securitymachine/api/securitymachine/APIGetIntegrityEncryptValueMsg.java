package org.zstack.header.securitymachine.api.securitymachine;
import org.springframework.http.HttpMethod;
import org.zstack.header.identity.SuppressCredentialCheck;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/integrity/encrypt/value/get",
        method = HttpMethod.POST,
        responseClass = APIGetIntegrityEncryptValueEvent.class,
        parameterName = "params"
)
@SuppressCredentialCheck
public class APIGetIntegrityEncryptValueMsg extends APIMessage {
    @APIParam
    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public static APIGetIntegrityEncryptValueMsg __example__() {
        APIGetIntegrityEncryptValueMsg msg = new APIGetIntegrityEncryptValueMsg();
        msg.setText(uuid());
        return msg;
    }
}
