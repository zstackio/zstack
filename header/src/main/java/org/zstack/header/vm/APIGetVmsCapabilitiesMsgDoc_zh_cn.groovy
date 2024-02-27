package org.zstack.header.vm

import org.zstack.header.vm.APIGetVmsCapabilitiesReply

doc {
    title "GetVmsCapabilities"

    category "vmInstance"

    desc """批量获取云主机能力"""

    rest {
        request {
			url "GET /v1/vm-instances/capabilities"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIGetVmsCapabilitiesMsg.class

            desc """"""
            
			params {

				column {
					name "vmUuids"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional false
					since "4.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "4.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "4.0"
				}
			}
        }

        response {
            clz APIGetVmsCapabilitiesReply.class
        }
    }
}