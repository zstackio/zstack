package org.zstack.header.server

import org.zstack.header.server.APIUpdateProvisionNetworkEvent

doc {
    title "UpdateProvisionNetwork"

    category "physicalServer"

    desc """在这里填写API描述"""

    rest {
        request {
			url "PUT /v1/provision-networks/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIUpdateProvisionNetworkMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn "updateProvisionNetwork"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "5.5.16"
				}
				column {
					name "name"
					enclosedIn "updateProvisionNetwork"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "description"
					enclosedIn "updateProvisionNetwork"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "dhcpInterface"
					enclosedIn "updateProvisionNetwork"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "dhcpRangeStartIp"
					enclosedIn "updateProvisionNetwork"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "dhcpRangeEndIp"
					enclosedIn "updateProvisionNetwork"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "dhcpRangeNetmask"
					enclosedIn "updateProvisionNetwork"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "dhcpRangeGateway"
					enclosedIn "updateProvisionNetwork"
					desc ""
					location "body"
					type "String"
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
            clz APIUpdateProvisionNetworkEvent.class
        }
    }
}