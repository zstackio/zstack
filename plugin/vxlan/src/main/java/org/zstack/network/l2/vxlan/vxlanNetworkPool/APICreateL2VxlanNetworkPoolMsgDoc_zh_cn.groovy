package org.zstack.network.l2.vxlan.vxlanNetworkPool

import org.zstack.network.l2.vxlan.vxlanNetworkPool.APICreateL2VxlanNetworkPoolEvent

doc {
    title "CreateL2VxlanNetworkPool"

    category "network.l2"

    desc """在这里填写API描述"""

    rest {
        request {
			url "POST /v1/l2-networks/vxlan-pool"

			header (Authorization: 'OAuth the-session-uuid')

            clz APICreateL2VxlanNetworkPoolMsg.class

            desc """"""
            
			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "physicalInterface"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "type"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "vSwitchType"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "4.1.2"
					values ("LinuxBridge","OvsDpdk")
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
				column {
					name "isolated"
					enclosedIn "params"
					desc ""
					location "body"
					type "Boolean"
					optional true
					since "4.8.0"
				}
				column {
					name "pvlan"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "4.8.0"
				}
			}
        }

        response {
            clz APICreateL2VxlanNetworkPoolEvent.class
        }
    }
}