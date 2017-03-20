package org.zstack.header.zone;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 6/14/2015.
 */
@RestRequest(
        path = "/zones/{uuid}/actions",
        isAction = true,
        responseClass = APIUpdateZoneEvent.class,
        method = HttpMethod.PUT
)
public class APIUpdateZoneMsg extends APIMessage implements ZoneMessage {
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(resourceType = ZoneVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getZoneUuid() {
        return uuid;
    }
 
    public static APIUpdateZoneMsg __example__() {
        APIUpdateZoneMsg msg = new APIUpdateZoneMsg();
        msg.setName("TestZone2");
        msg.setDescription("test second zone");
        msg.setUuid(uuid());
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("updated")
                        .resource(uuid,ZoneVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }

}
