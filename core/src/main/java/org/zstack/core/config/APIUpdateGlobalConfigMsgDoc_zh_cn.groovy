package org.zstack.core.config



doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "PUT /v1/global-configurations/{category}/{name}/actions"


            header (OAuth: 'the-session-uuid')

            clz APIUpdateGlobalConfigMsg.class

            desc ""
            
			params {

				column {
					name "category"
					enclosedIn "updateGlobalConfig"
					desc ""
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "name"
					enclosedIn "updateGlobalConfig"
					desc "资源名称"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "value"
					enclosedIn "updateGlobalConfig"
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
            clz APIUpdateGlobalConfigEvent.class
        }
    }
}