package org.zstack.header.network.l3;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

/**
 * @api create l3Network
 * @category l3Network
 * @cli
 * @httpMsg {
 * "org.zstack.header.network.l3.APICreateL3NetworkMsg": {
 * "name": "GuestNetwork",
 * "description": "Test",
 * "type": "L3BasicNetwork",
 * "l2NetworkUuid": "2f5e0755584d41dabb73c7dcbee2fe29",
 * "session": {
 * "uuid": "ec153083110d4b508bebb8750b62a393"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.network.l3.APICreateL3NetworkMsg": {
 * "name": "GuestNetwork",
 * "description": "Test",
 * "type": "L3BasicNetwork",
 * "l2NetworkUuid": "2f5e0755584d41dabb73c7dcbee2fe29",
 * "session": {
 * "uuid": "ec153083110d4b508bebb8750b62a393"
 * },
 * "timeout": 1800000,
 * "id": "d0248f2636af4d4cb93667f55e67b560",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APICreateL3NetworkEvent`
 * @since 0.1.0
 */
@TagResourceType(L3NetworkVO.class)
@Action(category = L3NetworkConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/l3-networks",
        method = HttpMethod.POST,
        responseClass = APICreateL3NetworkEvent.class,
        parameterName = "params"
)
public class APICreateL3NetworkMsg extends APICreateMessage {
    /**
     * @desc max length of 255 characters
     */
    @APIParam(maxLength = 255)
    private String name;
    /**
     * @desc max length of 2048 characters
     */
    @APIParam(required = false, maxLength = 2048)
    private String description;
    /**
     * @desc l3Network type
     */
    private String type = L3NetworkConstant.L3_BASIC_NETWORK_TYPE;
    /**
     * @desc uuid of l2Network the l3Network is being created on
     */
    @APIParam(resourceType = L2NetworkVO.class)
    private String l2NetworkUuid;

    private boolean system;

    private String dnsDomain;

    public String getDnsDomain() {
        return dnsDomain;
    }

    public void setDnsDomain(String domain) {
        this.dnsDomain = domain;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }
 
    public static APICreateL3NetworkMsg __example__() {
        APICreateL3NetworkMsg msg = new APICreateL3NetworkMsg();

        msg.setName("Test-L3Network");
        msg.setL2NetworkUuid(uuid());

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                if (evt.isSuccess()) {
                    ntfy("Created").resource(((APICreateL3NetworkEvent)evt).getInventory().getUuid(), L3NetworkVO.class.getSimpleName())
                            .messageAndEvent(that, evt).done();
                }
            }
        };
    }

}
