package org.zstack.header.server

import org.zstack.header.server.APIQueryProvisionNetworkReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryProvisionNetwork"

    category "physicalServer"

    desc """在这里填写API描述"""

    rest {
        request {
			url "GET /v1/provision-networks"
			url "GET /v1/provision-networks/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIQueryProvisionNetworkMsg.class

            desc """"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryProvisionNetworkReply.class
        }
    }
}