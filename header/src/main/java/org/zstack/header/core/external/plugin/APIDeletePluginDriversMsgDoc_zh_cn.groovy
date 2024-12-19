package org.zstack.header.core.external.plugin

import org.zstack.header.core.external.plugin.APIRefreshPluginDriversEvent

doc {
    title "DeletePluginDrivers"

    category "external.plugin"

    desc """删除插件驱动器"""

    rest {
        request {
			url "DELETE /v1/external/plugins/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIDeletePluginDriversMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
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
			}
        }

        response {
            clz APIRefreshPluginDriversEvent.class
        }
    }
}