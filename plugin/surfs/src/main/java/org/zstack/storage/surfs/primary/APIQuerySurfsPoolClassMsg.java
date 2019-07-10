package org.zstack.storage.surfs.primary;
import static java.util.Arrays.asList;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;


@AutoQuery(replyClass = APIQuerySurfsPoolClassReplay.class, inventoryClass = SurfsPoolClassInventory.class)
@RestRequest(
        path = "/primary-storage/surfs/poolclss",
        optionalPaths = {"/primary-storage/surfs/poolclss/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQuerySurfsPoolClassReplay.class
)
public class APIQuerySurfsPoolClassMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList();
    }

}