package org.zstack.header.server

import org.zstack.header.server.APIUpdateServerPoolEvent

doc {
    title "UpdateServerPool"

    category "physicalServer"

    desc """在这里填写API描述"""

    rest {
        request {
			url "PUT /v1/server-pools/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIUpdateServerPoolMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn "updateServerPool"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "5.5.16"
				}
				column {
					name "name"
					enclosedIn "updateServerPool"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "description"
					enclosedIn "updateServerPool"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "physicalLocation"
					enclosedIn "updateServerPool"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "networkTopology"
					enclosedIn "updateServerPool"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.5.16"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.5.16"
				}
			}
        }

        response {
            clz APIUpdateServerPoolEvent.class
        }
    }
}