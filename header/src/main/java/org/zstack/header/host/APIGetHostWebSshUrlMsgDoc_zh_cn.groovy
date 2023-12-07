package org.zstack.header.host

import org.zstack.header.host.APIGetHostWebSshUrlEvent

doc {
    title "GetHostWebSshUrl"

    category "host"

    desc """获取物理机网页终端链接"""

    rest {
        request {
			url "POST /v1/hosts/webssh"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIGetHostWebSshUrlMsg.class

            desc """获取一台物理机网页终端链接"""
            
			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "资源的UUID，唯一标示该资源"
					location "body"
					type "String"
					optional false
					since "4.7.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.7.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.7.0"
				}
				column {
					name "https"
					enclosedIn "params"
					desc "是否是HTTPS"
					location "body"
					type "Boolean"
					optional true
					since "4.7.11"
				}
			}
        }

        response {
            clz APIGetHostWebSshUrlEvent.class
        }
    }
}