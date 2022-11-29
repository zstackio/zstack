package org.zstack.network.l2.vxlan.vtep

import org.zstack.network.l2.vxlan.vtep.APICreateVxlanVtepEvent

doc {
    title "CreateVxlanVtep"

    category "network.l2"

    desc """创建vxlan隧道端点"""

    rest {
        request {
			url "POST /v1/l2-networks/vxlan/vteps"

			header (Authorization: 'OAuth the-session-uuid')

            clz APICreateVxlanVtepMsg.class

            desc """"""
            
			params {

				column {
					name "hostUuid"
					enclosedIn "params"
					desc "物理机UUID"
					location "body"
					type "String"
					optional false
					since "3.0"
				}
				column {
					name "poolUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "3.0"
				}
				column {
					name "vtepIp"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
			}
        }

        response {
            clz APICreateVxlanVtepEvent.class
        }
    }
}