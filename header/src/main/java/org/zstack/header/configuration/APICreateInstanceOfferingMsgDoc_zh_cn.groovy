package org.zstack.header.configuration



doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "POST /v1/instance-offerings"


            header (OAuth: 'the-session-uuid')

            clz APICreateInstanceOfferingMsg.class

            desc ""
            
			params {

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
					name "cpuNum"
					enclosedIn "params"
					desc ""
					location "body"
					type "int"
					optional false
					since "0.6"
					
				}
				column {
					name "cpuSpeed"
					enclosedIn "params"
					desc ""
					location "body"
					type "int"
					optional false
					since "0.6"
					
				}
				column {
					name "memorySize"
					enclosedIn "params"
					desc ""
					location "body"
					type "long"
					optional false
					since "0.6"
					
				}
				column {
					name "allocatorStrategy"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "sortKey"
					enclosedIn "params"
					desc ""
					location "body"
					type "int"
					optional true
					since "0.6"
					
				}
				column {
					name "type"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
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
            clz APICreateInstanceOfferingEvent.class
        }
    }
}