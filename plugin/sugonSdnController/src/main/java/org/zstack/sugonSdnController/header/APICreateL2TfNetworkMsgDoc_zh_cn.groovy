package org.zstack.sugonSdnController.header

import org.zstack.header.network.l2.APICreateL2NetworkEvent

doc {
    title "创建tungsten fabric二层网络(CreateL2TfNetwork)"

    category "network.l2"

    desc """创建tungsten fabric二层网络"""

    rest {
        request {
			url "POST /v1/l2-networks/tf"

			header (Authorization: 'OAuth the-session-uuid')

            clz APICreateL2TfNetworkMsg.class

            desc """"""
            
			params {

				column {
					name "ipPrefix"
					enclosedIn "params"
					desc "IP地址前缀"
					location "body"
					type "String"
					optional true
					since "4.8.0"
				}
				column {
					name "ipPrefixLength"
					enclosedIn "params"
					desc "IP地址前缀长度"
					location "body"
					type "Integer"
					optional true
					since "4.8.0"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "4.8.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "4.8.0"
				}
				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional false
					since "4.8.0"
				}
				column {
					name "physicalInterface"
					enclosedIn "params"
					desc "物理网卡"
					location "body"
					type "String"
					optional false
					since "4.8.0"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "二层网络类型"
					location "body"
					type "String"
					optional true
					since "4.8.0"
				}
				column {
					name "vSwitchType"
					enclosedIn "params"
					desc "虚拟交换机类型"
					location "body"
					type "String"
					optional true
					since "4.8.0"
					values ("LinuxBridge","OvsDpdk","MacVlan")
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "4.8.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "4.8.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.8.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.8.0"
				}
			}
        }

        response {
            clz APICreateL2NetworkEvent.class
        }
    }
}