package org.zstack.header.core.external.service

import org.zstack.header.core.external.service.APIDeleteExternalServiceConfigurationEvent

doc {
    title "DeleteExternalServiceConfiguration"

    category "externalService"

    desc """删除外部服务配置"""

    rest {
        request {
			url "DELETE /v1/external/service/configuration/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIDeleteExternalServiceConfigurationMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "5.4.6"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "body"
					type "String"
					optional true
					since "5.4.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.4.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.4.6"
				}
			}
        }

        response {
            clz APIDeleteExternalServiceConfigurationEvent.class
        }
    }
}