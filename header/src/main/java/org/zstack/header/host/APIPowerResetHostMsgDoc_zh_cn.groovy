package org.zstack.header.host

import org.zstack.header.host.APIPowerResetHostEvent

doc {
    title "PowerResetHost"

    category "host"

    desc """重启物理机"""

    rest {
        request {
			url "PUT /v1/hosts/power/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIPowerResetHostMsg.class

            desc """重启一台物理机"""
            
			params {

				column {
					name "uuid"
					enclosedIn "powerResetHost"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"

				}
				column {
					name "method"
					enclosedIn "powerResetHost"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"

					values ("AUTO","AGENT","IPMI")
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
            clz APIPowerResetHostEvent.class
        }
    }
}