package org.zstack.header.storage.primary;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

/**
 * @api
 *
 * change primary storage state
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 *{
"org.zstack.header.storage.primary.APIChangePrimaryStorageStateMsg": {
"uuid": "e330607585a54a99a0dd7c1351e3ae73",
"stateEvent": "enable",
"session": {
"uuid": "fea5820d34274d5d90564c23429b97b8"
}
}
}
 *
 * @msg
 *
 * {
"org.zstack.header.storage.primary.APIChangePrimaryStorageStateMsg": {
"uuid": "e330607585a54a99a0dd7c1351e3ae73",
"stateEvent": "enable",
"session": {
"uuid": "fea5820d34274d5d90564c23429b97b8"
},
"timeout": 1800000,
"id": "ffabcef2005343bc8c8388fb4eafda2b",
"serviceId": "api.portal"
}
}
 *
 * @result
 *
 * see :ref:`APIChangePrimaryStorageStateEvent`
 */
public class APIChangePrimaryStorageStateMsg extends APIMessage implements PrimaryStorageMessage {
    /**
     * @desc
     * primary storage uuid
     */
	@APIParam(resourceType = PrimaryStorageVO.class)
	private String uuid;
    /**
     * @desc
     * - enable: enable primary storage
     * - disable: disable primary storage
     *
     * for details of primary storage states, see state of :ref:`PrimaryStorageInventory`
     */
	@APIParam(validValues = {"enable", "disable", "maintain"})
	private String stateEvent;

	public APIChangePrimaryStorageStateMsg(String uuid, String stateEvent) {
		super();
		this.uuid = uuid;
		this.stateEvent = stateEvent;
	}

	public APIChangePrimaryStorageStateMsg() {
	}
	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
    @Override
    public String getPrimaryStorageUuid() {
        return getUuid();
    }
	public String getStateEvent() {
		return stateEvent;
	}

	public void setStateEvent(String stateEvent) {
		this.stateEvent = stateEvent;
	}
}
