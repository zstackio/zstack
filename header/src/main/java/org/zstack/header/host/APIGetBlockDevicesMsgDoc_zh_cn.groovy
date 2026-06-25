package org.zstack.header.host

import org.zstack.header.host.APIGetBlockDevicesEvent

doc {
    title "GetBlockDevices"

    category "host"

    desc """获取指定物理机上的块设备列表"""

    rest {
        request {
			url "GET /v1/hosts/{uuid}/block-devices"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIGetBlockDevicesMsg.class

            desc """获取指定物理机上的块设备列表"""
            
			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "5.5.28"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "5.5.28"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "5.5.28"
				}
				column {
					name "includeInUse"
					enclosedIn ""
					desc "是否包含已被系统使用的块设备"
					location "query"
					type "boolean"
					optional true
					since "5.5.28"
				}
			}
        }

        response {
            clz APIGetBlockDevicesEvent.class
        }
    }
}