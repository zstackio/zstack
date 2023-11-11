package org.zstack.sdnController.header

import org.zstack.sdnController.header.APICreateL2HardwareVxlanNetworkEvent

doc {
    title "CreateL2HardwareVxlanNetwork"

    category "network.l2"

    desc """创建硬件VXLAN网络"""

    rest {
        request {
			url "POST /v1/l2-networks/hardware-vxlan"

			header (Authorization: 'OAuth the-session-uuid')

            clz APICreateL2HardwareVxlanNetworkMsg.class

            desc """"""
            
			params {

				column {
					name "vni"
					enclosedIn "params"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "3.7"
				}
				column {
					name "poolUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "3.7"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "3.7"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.7"
				}
				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional false
					since "3.7"
				}
				column {
					name "physicalInterface"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "3.7"
				}
				column {
					name "type"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.7"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.7"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.7"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.7"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.7"
				}
				column {
					name "vSwitchType"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("LinuxBridge","OvsDpdk")
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
            clz APICreateL2HardwareVxlanNetworkEvent.class
        }
    }
}