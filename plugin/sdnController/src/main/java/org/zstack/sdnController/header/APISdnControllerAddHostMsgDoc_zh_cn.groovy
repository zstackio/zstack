package org.zstack.sdnController.header

import org.zstack.sdnController.header.APISdnControllerAddHostEvent

doc {
    title "SdnControllerAddHost"

    category "SdnController"

    desc """SDN控制器添加物理机"""

    rest {
        request {
			url "POST /v1/sdn-controllers/{sdnControllerUuid}/hosts/{hostUuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APISdnControllerAddHostMsg.class

            desc """"""
            
			params {

				column {
					name "sdnControllerUuid"
					enclosedIn ""
					desc "SDN控制器Uuid"
					location "url"
					type "String"
					optional false
					since "5.3.0"
				}
				column {
					name "hostUuid"
					enclosedIn ""
					desc "物理机UUID"
					location "url"
					type "String"
					optional false
					since "5.3.0"
				}
				column {
					name "vSwitchType"
					enclosedIn ""
					desc "虚拟交换机类型"
					location "body"
					type "String"
					optional true
					since "5.3.0"
					values ("OvsKernel","OvsDpdk","SRIOV")
				}
				column {
					name "nicNames"
					enclosedIn ""
					desc "物理机网卡名称列表"
					location "body"
					type "List"
					optional false
					since "5.3.0"
				}
				column {
					name "vtepIp"
					enclosedIn ""
					desc "物理机VTEP IP"
					location "body"
					type "String"
					optional true
					since "5.3.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.3.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.3.0"
				}
				column {
					name "netmask"
					enclosedIn ""
					desc "物理机VTEP IP掩码"
					location "body"
					type "String"
					optional true
					since "5.3.0"
				}
				column {
					name "bondMode"
					enclosedIn ""
					desc "物理机网卡bond模式"
					location "body"
					type "String"
					optional true
					since "5.3.0"
				}
				column {
					name "lacpMode"
					enclosedIn ""
					desc "物理机网卡LACP模式"
					location "body"
					type "String"
					optional true
					since "5.3.0"
				}
			}
        }

        response {
            clz APISdnControllerAddHostEvent.class
        }
    }
}