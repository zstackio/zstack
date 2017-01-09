package org.zstack.ldap

org.zstack.ldap.APIUpdateLdapServerEvent

doc {
    title "UpdateLdapServer"

    category "ldap"

    desc "在这里填写API描述"

    rest {
        request {
			url "PUT /v1/ldap/servers/{ldapServerUuid}"


            header (OAuth: 'the-session-uuid')

            clz APIUpdateLdapServerMsg.class

            desc ""
            
			params {

				column {
					name "ldapServerUuid"
					enclosedIn "updateLdapServer"
					desc ""
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "name"
					enclosedIn "updateLdapServer"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "description"
					enclosedIn "updateLdapServer"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "url"
					enclosedIn "updateLdapServer"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "base"
					enclosedIn "updateLdapServer"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "username"
					enclosedIn "updateLdapServer"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "password"
					enclosedIn "updateLdapServer"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "encryption"
					enclosedIn "updateLdapServer"
					desc ""
					location "body"
					type "String"
					optional true
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
            clz APIUpdateLdapServerEvent.class
        }
    }
}