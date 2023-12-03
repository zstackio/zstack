package org.zstack.header.identity

import org.zstack.header.identity.APIGetSupportedIdentityModelsReply

doc {
    title "GetSupportedIdentityModels"

    category "identity"

    desc """获取支持的账户类型"""

    rest {
        request {
			url "GET /v1/identity-models"



            clz APIGetSupportedIdentityModelsMsg.class

            desc """"""
            
			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.7.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.7.0"
				}
			}
        }

        response {
            clz APIGetSupportedIdentityModelsReply.class
        }
    }
}