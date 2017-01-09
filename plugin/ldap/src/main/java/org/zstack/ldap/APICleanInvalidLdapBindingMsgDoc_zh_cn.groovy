package org.zstack.ldap

org.zstack.ldap.APICleanInvalidLdapBindingEvent

doc {
    title "CleanInvalidLdapBinding"

    category "ldap"

    desc "在这里填写API描述"

    rest {
        request {
			url "PUT /v1/ldap/bindings/actions"


            header (OAuth: 'the-session-uuid')

            clz APICleanInvalidLdapBindingMsg.class

            desc ""
            
			params {

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
            clz APICleanInvalidLdapBindingEvent.class
        }
    }
}