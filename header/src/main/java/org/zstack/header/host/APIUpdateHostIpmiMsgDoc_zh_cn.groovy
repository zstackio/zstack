package org.zstack.header.host

import org.zstack.header.host.APIUpdateHostIpmiEvent

doc {
    title "UpdateHostIpmi"

    category "host"

    desc """更新物理机IPMI信息"""

    rest {
        request {
			url "PUT /v1/hosts/ipmi/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIUpdateHostIpmiMsg.class

            desc """更新一台物理机IPMI信息"""
            
			params {

				column {
					name "uuid"
					enclosedIn "updateHostIpmi"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"

				}
				column {
					name "ipmiAddress"
					enclosedIn "updateHostIpmi"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"

				}
				column {
					name "ipmiUsername"
					enclosedIn "updateHostIpmi"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"

				}
				column {
					name "ipmiPassword"
					enclosedIn "updateHostIpmi"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"

				}
				column {
					name "ipmiPort"
					enclosedIn "updateHostIpmi"
					desc ""
					location "body"
					type "int"
					optional true
					since "0.6"

				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"

				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"

				}
			}
        }

        response {
            clz APIUpdateHostIpmiEvent.class
        }
    }
}