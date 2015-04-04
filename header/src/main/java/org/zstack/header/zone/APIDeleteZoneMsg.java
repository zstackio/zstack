package org.zstack.header.zone;

import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;

/**
 * @api
 * delete a zone. All descendant resources, for example cluster/host/vm, are deleted in
 * cascade as well
 *
 * @msg
{
"org.zstack.header.zone.APIDeleteZoneMsg": {
"uuid": "e60128363bb244bf8de1f7ddadc9632f",
"deleteMode": "Permissive",
"session": {
"uuid": "7d114b56078245dbb85bd72364949220"
},
"timeout": 1800000,
"id": "b10bfe3e9dc4446b9ca3474a61942c76",
"serviceId": "api.portal"
}
}

   @httpMsg

   {
   "org.zstack.header.zone.APIDeleteZoneMsg": {
   "session": {
   "uuid": "7d114b56078245dbb85bd72364949220"
   },
   "uuid": "e60128363bb244bf8de1f7ddadc9632f"
   }
   }

 * @since 0.1.0
 *
 * @cli
 *
 * @result
 * see :ref:`APIDeleteZoneEvent`
 */
public class APIDeleteZoneMsg extends APIDeleteMessage implements ZoneMessage {
    /**
     * @desc zone uuid
     */
    @APIParam(resourceType = ZoneVO.class)
	private String uuid;

	public APIDeleteZoneMsg() {
	}
	
	public APIDeleteZoneMsg(String uuid) {
	    super();
	    this.uuid = uuid;
    }

	public String getUuid() {
    	return uuid;
    }

	public void setUuid(String uuid) {
    	this.uuid = uuid;
    }

    @Override
    public String getZoneUuid() {
        return getUuid();
    }
}
