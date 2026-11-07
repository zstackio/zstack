package org.zstack.header.core.external.plugin

import org.zstack.header.core.external.plugin.APIAddFeaturePluginPackageEvent

doc {
    title "AddFeaturePluginPackage"

    category "external.plugin"

    desc """上传一个功能插件包（.zpkg）到插件仓库，之后可以用InstallFeaturePlugin安装"""

    rest {
        request {
			url "POST /v1/feature-plugins/packages"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIAddFeaturePluginPackageMsg.class

            desc """"""
            
			params {

				column {
					name "name"
					enclosedIn "params"
					desc "插件包文件名，以.zpkg结尾"
					location "body"
					type "String"
					optional false
					since "5.5.48"
				}
				column {
					name "content"
					enclosedIn "params"
					desc "插件包文件内容，Base64编码"
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
            clz APIAddFeaturePluginPackageEvent.class
        }
    }
}