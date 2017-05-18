package org.zstack.header.core.webhooks

import org.zstack.header.core.webhooks.APICreateWebhookEvent

doc {
    title "CreateWebhook"

    category "webhook"

    desc """在这里填写API描述"""

    rest {
        request {
			url "POST /v1/web-hooks"

			header (Authorization: 'OAuth the-session-uuid')


            clz APICreateWebhookMsg.class

            desc """"""
            
			params {

				column {
					name "name"
					enclosedIn ""
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "description"
					enclosedIn ""
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "url"
					enclosedIn ""
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "type"
					enclosedIn ""
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "opaque"
					enclosedIn ""
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "resourceUuid"
					enclosedIn ""
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APICreateWebhookEvent.class
        }
    }
}