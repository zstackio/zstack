package org.zstack.ldap



doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "POST /v1/ldap/servers"


            header (OAuth: 'the-session-uuid')

            clz APIAddLdapServerMsg.class

            desc ""
            
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
					optional false
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
					name "base"
					enclosedIn ""
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "username"
					enclosedIn ""
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "password"
					enclosedIn ""
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "encryption"
					enclosedIn ""
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
					values ("None","TLS")
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
            clz APIAddLdapServerEvent.class
        }
    }
}