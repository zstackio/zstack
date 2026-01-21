package org.zstack.header.core.external.service

import org.zstack.header.core.external.service.APIUpdateExternalServiceConfigurationEvent

doc {
    title "UpdateExternalServiceConfiguration"

    category "externalService"

    desc """在这里填写API描述"""

    rest {
        request {
			url "PUT /v1/external/service/configuration/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIUpdateExternalServiceConfigurationMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn "updateExternalServiceConfiguration"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "5.4.6"
				}
				column {
					name "description"
					enclosedIn "updateExternalServiceConfiguration"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional false
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
            clz APIUpdateExternalServiceConfigurationEvent.class
        }
    }
}