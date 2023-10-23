package org.zstack.sugonSdnController.header

import org.zstack.header.network.l2.APICreateL2NetworkEvent

doc {
    title "CreateL2TfNetwork"

    category "network.l2"

    desc """在这里填写API描述"""

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
					desc ""
					location "body"
					type "String"
					optional true
					since "4.8.0"
				}
				column {
					name "ipPrefixLength"
					enclosedIn "params"
					desc ""
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
					desc ""
					location "body"
					type "String"
					optional false
					since "4.8.0"
				}
				column {
					name "type"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "4.8.0"
				}
				column {
					name "vSwitchType"
					enclosedIn "params"
					desc ""
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
            clz APICreateL2NetworkEvent.class
        }
    }
}