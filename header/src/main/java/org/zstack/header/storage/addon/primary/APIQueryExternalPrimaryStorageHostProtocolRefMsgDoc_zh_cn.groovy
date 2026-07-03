package org.zstack.header.storage.addon.primary

import org.zstack.header.storage.addon.primary.APIQueryExternalPrimaryStorageHostProtocolRefReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryExternalPrimaryStorageHostProtocolRef"

    category "storage.primary"

    desc """查询外接主存储按协议的主机连通性"""

    rest {
        request {
			url "GET /v1/external-primary-storage/host-protocol-refs"
			url "GET /v1/external-primary-storage/{primaryStorageUuid}/host-protocol-refs"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIQueryExternalPrimaryStorageHostProtocolRefMsg.class

            desc """"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryExternalPrimaryStorageHostProtocolRefReply.class
        }
    }
}