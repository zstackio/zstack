package org.zstack.header.core.external.plugin

import org.zstack.header.core.external.plugin.APIUninstallFeaturePluginEvent

doc {
    title "UninstallFeaturePlugin"

    category "external.plugin"

    desc """卸载一个功能插件，所有管理节点不重启即卸下该插件。插件正在被使用，或插件声明加载后不可卸载时，拒绝卸载"""

    rest {
        request {
			url "PUT /v1/feature-plugins/{pluginId}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIUninstallFeaturePluginMsg.class

            desc """"""
            
			params {

				column {
					name "pluginId"
					enclosedIn "uninstallFeaturePlugin"
					desc "功能插件ID"
					location "url"
					type "String"
					optional false
					since "5.5.48"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.5.48"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.5.48"
				}
			}
        }

        response {
            clz APIUninstallFeaturePluginEvent.class
        }
    }
}