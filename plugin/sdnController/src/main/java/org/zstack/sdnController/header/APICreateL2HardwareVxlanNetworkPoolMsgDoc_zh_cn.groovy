package org.zstack.sdnController.header

import org.zstack.sdnController.header.APICreateL2HardwareVxlanNetworkPoolEvent

doc {
    title "CreateL2HardwareVxlanNetworkPool"

    category "network.l2"

    desc """创建硬件VXLAN资源池"""

    rest {
        request {
			url "POST /v1/l2-networks/hardware-vxlan-pool"

			header (Authorization: 'OAuth the-session-uuid')

            clz APICreateL2HardwareVxlanNetworkPoolMsg.class

            desc """"""
            
			params {

				column {
					name "sdnControllerUuid"
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
					desc "虚拟交换机类型"
					location "body"
					type "String"
					optional true
					since "4.1.2"
					values ("LinuxBridge","OvsDpdk")
				}
			}
        }

        response {
            clz APICreateL2HardwareVxlanNetworkPoolEvent.class
        }
    }
}