package org.zstack.header.tag

import org.zstack.header.tag.APICreateUserTagEvent

doc {
    title "CreateUserTag"

    category "tag"

    desc """创建用户标签"""

    rest {
        request {
			url "POST /v1/user-tags"

			header (Authorization: 'OAuth the-session-uuid')


            clz APICreateUserTagMsg.class

            desc """创建用户标签"""
            
			params {

				column {
					name "resourceType"
					enclosedIn "params"
					desc "当创建一个标签时, 用户必须制定标签所关联的资源类型"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "用户指定的资源UUID，若指定，系统不会为该资源随机分配UUID"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "tag"
					enclosedIn "params"
					desc "标签字符串"
					location "body"
					type "String"
					optional false
					since "0.6"
					
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
            clz APICreateUserTagEvent.class
        }
    }
}