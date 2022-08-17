package org.zstack.sdnController.header

import org.zstack.header.query.APIQueryMessage

doc {
    title "QuerySdnController"

    category "SdnController"

    desc """查询SDN控制器"""

    rest {
        request {
			url "GET /v1/sdn-controllers"
			url "GET /v1/sdn-controllers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIQuerySdnControllerMsg.class

            desc """"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQuerySdnControllerReply.class
        }
    }
}