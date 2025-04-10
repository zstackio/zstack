package org.zstack.header.core.external.plugin

import org.zstack.header.core.external.plugin.APIRefreshPluginDrviersEvent

doc {
    title "RefreshPluginDrivers"

    category "external.plugin"

    desc """刷新插件驱动器"""

    rest {
        request {
			url "PUT /v1/external/plugins"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIRefreshPluginDriversMsg.class

            desc """"""
            
			params {

				column {
					name "name"
					enclosedIn "refreshPluginDrivers"
					desc "资源名称"
					location "body"
					type "String"
					optional true
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
            clz APIRefreshPluginDrviersEvent.class
        }
    }
}