package org.zstack.header.core.external.plugin

import org.zstack.header.core.external.plugin.APIGetFeaturePluginPackagesReply

doc {
    title "GetFeaturePluginPackages"

    category "external.plugin"

    desc """获取插件仓库中的功能插件包，包括随安装介质提供的和用户上传的"""

    rest {
        request {
			url "GET /v1/feature-plugins/packages"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIGetFeaturePluginPackagesMsg.class

            desc """"""
            
			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "5.5.48"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "5.5.48"
				}
			}
        }

        response {
            clz APIGetFeaturePluginPackagesReply.class
        }
    }
}