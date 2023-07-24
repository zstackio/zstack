package org.zstack.kvm.xmlhook;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryVmUserDefinedXmlHookScriptReply.class, inventoryClass = XmlHookInventory.class)
@RestRequest(
        path = "/vm-instances/xml-hook-script",
        optionalPaths = {"/vm-instances/xml-hook-script/{uuid}"},
        responseClass = APIQueryVmUserDefinedXmlHookScriptReply.class,
        method = HttpMethod.GET
)
public class APIQueryVmUserDefinedXmlHookScriptMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("uuid=xxx", "name=xxx");
    }
}
