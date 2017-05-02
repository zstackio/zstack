package org.zstack.header.network.l2;


import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.zone.ZoneVO;

/**
 * @api create l2Network
 * @category l2Network
 * @cli
 * @httpMsg {
 * "org.zstack.header.network.l2.APICreateL2NetworkMsg": {
 * "name": "TestL2Network",
 * "description": "Test",
 * "zoneUuid": "48c5febd96024e33809cc98035d79277",
 * "physicalInterface": "eth0",
 * "type": "L2NoVlanNetwork",
 * "session": {
 * "uuid": "d93f354c4339450e8c2a4c31de89da15"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.network.l2.APICreateL2NetworkMsg": {
 * "name": "TestL2Network",
 * "description": "Test",
 * "zoneUuid": "48c5febd96024e33809cc98035d79277",
 * "physicalInterface": "eth0",
 * "type": "L2NoVlanNetwork",
 * "session": {
 * "uuid": "d93f354c4339450e8c2a4c31de89da15"
 * },
 * "timeout": 1800000,
 * "id": "7b58a8e291e54d41bc3fe643bb1c76b4",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APICreateL2NetworkEvent`
 * @since 0.1.0
 */

public abstract class APICreateL2NetworkMsg extends APICreateMessage {
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
     * @desc zone uuid. See :ref:`ZoneInventory`
     */
    @APIParam(resourceType = ZoneVO.class)
    private String zoneUuid;
    /**
     * @desc physical interface name. See physicalInterface of :ref:`L2NetworkInventory` for details
     */
    @APIParam(maxLength = 1024)
    private String physicalInterface;
    /**
     * @desc l2Network type
     */
    private String type;

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

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getPhysicalInterface() {
        return physicalInterface;
    }

    public void setPhysicalInterface(String physicalInterface) {
        this.physicalInterface = physicalInterface;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                if (evt.isSuccess()) {
                    ntfy("Created").resource(((APICreateL2NetworkEvent)evt).getInventory().getUuid(), L2NetworkVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
                }
            }
        };
    }

}