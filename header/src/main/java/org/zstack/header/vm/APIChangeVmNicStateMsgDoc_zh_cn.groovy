package org.zstack.header.vm

doc {
    title "ChangeVmNicState"

    category "vmInstance"

    desc """修改云主机网卡状态"""

    rest {
        request {
			url "PUT /v1/vm-instances/nics/{vmNicUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIChangeVmNicStateMsg.class

            desc """"""
            
			params {

				column {
					name "vmNicUuid"
					enclosedIn ""
					desc "云主机网卡UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "state"
					enclosedIn ""
					desc "云主机网卡状态"
					location "body"
					type "String"
					optional false
					since "4.5"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIChangeVmNicStateEvent.class
        }
    }
}