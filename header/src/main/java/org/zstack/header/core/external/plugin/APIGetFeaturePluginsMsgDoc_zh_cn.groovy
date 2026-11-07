package org.zstack.header.core.external.plugin

import org.zstack.header.core.external.plugin.APIGetFeaturePluginsReply

doc {
    title "GetFeaturePlugins"

    category "external.plugin"

    desc """获取已安装的功能插件及其状态"""

    rest {
        request {
			url "GET /v1/feature-plugins"



            clz APIGetFeaturePluginsMsg.class

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
            clz APIGetFeaturePluginsReply.class
        }
    }
}