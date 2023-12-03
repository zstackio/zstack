package org.zstack.header.vm

import org.zstack.header.vm.APIDeleteVmNicEvent

doc {
    title "DeleteVmNic"

    category "vmInstance"

    desc """删除云主机网卡"""

    rest {
        request {
			url "DELETE /v1/nics/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIDeleteVmNicMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "4.0"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc ""
					location "body"
					type "String"
					optional true
					since "4.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "4.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "4.0"
				}
			}
        }

        response {
            clz APIDeleteVmNicEvent.class
        }
    }
}