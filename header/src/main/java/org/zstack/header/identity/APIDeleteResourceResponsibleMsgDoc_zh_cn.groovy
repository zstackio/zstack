package org.zstack.header.identity

import org.zstack.header.identity.APIDeleteResourceResponsibleEvent

doc {
    title "DeleteResourceResponsible"

    category "identity"

    desc """删除资源责任关系"""

    rest {
        request {
			url "DELETE /v1/resources/responsible/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIDeleteResourceResponsibleMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "body"
					type "String"
					optional true
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
            clz APIDeleteResourceResponsibleEvent.class
        }
    }
}