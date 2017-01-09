package org.zstack.appliancevm

import org.zstack.appliancevm.APIQueryApplianceVmReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryApplianceVm"

    category "applianceVm"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/vm-instances/appliances"

			url "GET /v1/vm-instances/appliances/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQueryApplianceVmMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryApplianceVmReply.class
        }
    }
}