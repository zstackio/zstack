package org.zstack.header.host

import org.zstack.header.host.APIGetBlockDevicesEvent

doc {
    title "GetBlockDevices"

    category "未知类别"

    desc """在这里填写API描述"""

    rest {
        request {
			url "GET /v1/hosts/{uuid}/block-devices"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIGetBlockDevicesMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "5.5.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "5.5.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "5.5.6"
				}
			}
        }

        response {
            clz APIGetBlockDevicesEvent.class
        }
    }
}