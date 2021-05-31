package org.zstack.header.acl

import org.zstack.header.acl.APIChangeAccessControlListRedirectRuleEvent

doc {
    title "ChangeAccessControlListRedirectRule"

    category "acl"

    desc """在这里填写API描述"""

    rest {
        request {
			url "PUT /v1/access-control-lists/{aclUuid}/redirectRules/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIChangeAccessControlListRedirectRuleMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn "changeAccessControlListRedirectRule"
					desc "资源的UUID，唯一标示该资源"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "name"
					enclosedIn "changeAccessControlListRedirectRule"
					desc "资源名称"
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
            clz APIChangeAccessControlListRedirectRuleEvent.class
        }
    }
}