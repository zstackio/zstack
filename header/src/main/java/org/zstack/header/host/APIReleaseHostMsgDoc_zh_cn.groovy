package org.zstack.header.host

import org.zstack.header.host.APIReleaseHostEvent

doc {
    title "释放主机信息"

    category "host"

    desc """接受释放物理机"""

    rest {
        request {
			url "POST /v1/hosts/release"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIReleaseHostMsg.class

            desc """"""
            
			params {

				column {
					name "productUuid"
					enclosedIn "params"
					desc "主板uuid"
					location "body"
					type "String"
					optional false
					since "5.4.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.4.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.4.6"
				}
			}
        }

        response {
            clz APIReleaseHostEvent.class
        }
    }
}