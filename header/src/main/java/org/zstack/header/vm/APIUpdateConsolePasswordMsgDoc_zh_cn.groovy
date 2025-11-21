package org.zstack.header.vm

import org.zstack.header.vm.APIUpdateConsolePasswordEvent

doc {
    title "UpdateConsolePassword"

    category "vmInstance"

    desc """更新虚拟机控制台密码，虚拟机必须处于运行状态且已设置控制台密码"""

    rest {
        request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIUpdateConsolePasswordMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn "updateConsolePassword"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "5.4.2"
				}
				column {
					name "password"
					enclosedIn "updateConsolePassword"
					desc ""
					location "body"
					type "String"
					optional false
					since "5.4.2"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.4.2"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.4.2"
				}
			}
        }

        response {
            clz APIUpdateConsolePasswordEvent.class
        }
    }
}