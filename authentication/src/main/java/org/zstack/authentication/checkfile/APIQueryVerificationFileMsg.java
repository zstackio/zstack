package org.zstack.authentication.checkfile;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.identity.Action;
import static java.util.Arrays.asList;
import java.util.List;

@AutoQuery(replyClass = APIQueryVerificationFileReply.class, inventoryClass = FileVerificationInventory.class)
@RestRequest(
        path = "/authentication/file",
        method = HttpMethod.GET,
        responseClass = APIQueryVerificationFileReply.class
)
public class APIQueryVerificationFileMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList();
    }
}
