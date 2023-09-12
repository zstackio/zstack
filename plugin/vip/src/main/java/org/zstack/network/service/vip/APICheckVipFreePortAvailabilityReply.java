package org.zstack.network.service.vip;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import java.util.ArrayList;
import java.sql.Timestamp;
import org.zstack.header.message.APIReply;
import java.util.List;

@RestResponse(fieldsTo = {"all"})
public class APICheckVipFreePortAvailabilityReply extends APIReply {

    private boolean available;

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public static APICheckVipFreePortAvailabilityReply __example__() {
        APICheckVipFreePortAvailabilityReply reply = new APICheckVipFreePortAvailabilityReply();
        reply.setAvailable(true);
        return reply;
    }

}
