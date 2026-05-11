package org.zstack.header.server

import org.zstack.header.server.APICreateProvisionNetworkEvent

doc {
    title "CreateProvisionNetwork"

    category "physicalServer"

    desc """在这里填写API描述"""

    rest {
        request {
			url "POST /v1/provision-networks"

			header (Authorization: 'OAuth the-session-uuid')

            clz APICreateProvisionNetworkMsg.class

            desc """"""
            
			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "5.5.16"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional false
					since "5.5.16"
				}
				column {
					name "type"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "5.5.16"
					values ("STANDALONE_PXE","GATEWAY_PXE")
				}
				column {
					name "dhcpInterface"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "dhcpRangeStartIp"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "dhcpRangeEndIp"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "dhcpRangeNetmask"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "dhcpRangeGateway"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "5.5.16"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.5.16"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.5.16"
				}
			}
        }

        response {
            clz APICreateProvisionNetworkEvent.class
        }
    }
}