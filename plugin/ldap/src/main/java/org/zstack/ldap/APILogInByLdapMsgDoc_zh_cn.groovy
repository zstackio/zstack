package org.zstack.ldap

org.zstack.ldap.APILogInByLdapReply

doc {
    title "LogInByLdap"

    category "ldap"

    desc "在这里填写API描述"

    rest {
        request {
			url "PUT /v1/ldap/login"


            

            clz APILogInByLdapMsg.class

            desc ""
            
			params {

				column {
					name "uid"
					enclosedIn "logInByLdap"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "password"
					enclosedIn "logInByLdap"
					desc ""
					location "body"
					type "String"
					optional false
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
            clz APILogInByLdapReply.class
        }
    }
}