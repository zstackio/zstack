package org.zstack.header.core.external.service

import org.zstack.header.core.external.service.APIAddExternalServiceConfigurationEvent

doc {
    title "新建外部服务配置"

    category "externalService"

    desc """新建外部服务配置"""

    rest {
        request {
			url "POST /v1/external/service/configuration"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIAddExternalServiceConfigurationMsg.class

            desc """"""
            
			params {

				column {
					name "externalServiceType"
					enclosedIn "params"
					desc "外部服务类型, 例如 Prometheus2"
					location "body"
					type "String"
					optional false
					since "5.5.12"
				}
				column {
					name "configuration"
					enclosedIn "params"
					desc "外部服务配置, 使用 json 格式"
					location "body"
					type "String"
					optional false
					since "5.5.12"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "5.5.12"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "5.5.12"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "5.5.12"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.5.12"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.5.12"
				}
			}
        }

        response {
            clz APIAddExternalServiceConfigurationEvent.class
        }
    }
}