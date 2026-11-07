package org.zstack.header.core.external.plugin

import org.zstack.header.core.external.plugin.APIInstallFeaturePluginEvent

doc {
    title "InstallFeaturePlugin"

    category "external.plugin"

    desc """安装插件仓库中的一个功能插件包。所有管理节点不重启即加载该插件，任一管理节点无法加载时撤销本次安装"""

    rest {
        request {
			url "POST /v1/feature-plugins"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIInstallFeaturePluginMsg.class

            desc """"""
            
			params {

				column {
					name "packagePath"
					enclosedIn "params"
					desc "插件包在插件仓库中的路径，即GetFeaturePluginPackages返回的path"
					location "body"
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
            clz APIInstallFeaturePluginEvent.class
        }
    }
}