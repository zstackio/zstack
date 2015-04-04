package org.zstack.header.cluster;

import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;

/**
 * @api
 * delete a cluster. All descendant resources, for example hosts/vm are deleted in cascade as well
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 *
 * {
"org.zstack.header.cluster.APIDeleteClusterMsg": {
"session": {
"uuid": "056414ac9bac43998b974c1af1670bea"
},
"uuid": "0f070d6a8c7748869edc3bb2bb538af2"
}
}
 *
 * @msg
 * {
"org.zstack.header.cluster.APIDeleteClusterMsg": {
"uuid": "0f070d6a8c7748869edc3bb2bb538af2",
"deleteMode": "Permissive",
"session": {
"uuid": "056414ac9bac43998b974c1af1670bea"
},
"timeout": 1800000,
"id": "1cd08e0b8da8415d91b3de68f3753035",
"serviceId": "api.portal"
}
}
 *
 * @result
 * see :ref:`APIDeleteClusterEvent`
 */
public class APIDeleteClusterMsg extends APIDeleteMessage implements ClusterMessage {
    /**
     * @desc cluster uuid
     */
	@APIParam
	private String uuid;

	public APIDeleteClusterMsg() {
	}
	
	public APIDeleteClusterMsg(String clusterUuid) {
		this.uuid = clusterUuid;
	}
	
	public void setUuid(String clusterUuid) {
    	this.uuid = clusterUuid;
    }

    public String getUuid() {
	    return uuid;
    }

    @Override
    public String getClusterUuid() {
        return getUuid();
    }
}
