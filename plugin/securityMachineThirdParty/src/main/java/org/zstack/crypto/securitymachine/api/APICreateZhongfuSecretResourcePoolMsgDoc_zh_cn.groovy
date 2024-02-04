package org.zstack.crypto.securitymachine.api

import org.zstack.header.securitymachine.api.secretresourcepool.APICreateSecretResourcePoolEvent

doc {
    title "CreateZhongfuSecretResourcePool"

    category "secretResourcePool"

    desc """在这里填写API描述"""

    rest {
        request {
			url "POST /v1/secret-resource-pool/zhongfu"

			header (Authorization: 'OAuth the-session-uuid')

            clz APICreateZhongfuSecretResourcePoolMsg.class

            desc """"""
            
			params {

				column {
					name "snNum"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "model"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "type"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
					values ("CloudSecurityMachine","OrdinarySecurityMachine")
				}
				column {
					name "heartbeatInterval"
					enclosedIn "params"
					desc ""
					location "body"
					type "Integer"
					optional false
					since "0.6"
					
				}
				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
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
            clz APICreateSecretResourcePoolEvent.class
        }
    }
}