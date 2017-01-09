package org.zstack.network.securitygroup

org.zstack.network.securitygroup.APIAttachSecurityGroupToL3NetworkEvent

doc {
    title "AttachSecurityGroupToL3Network"

    category "securityGroup"

    desc "在这里填写API描述"

    rest {
        request {
			url "POST /v1/security-groups/{securityGroupUuid}/l3-networks/{l3NetworkUuid}"


            header (OAuth: 'the-session-uuid')

            clz APIAttachSecurityGroupToL3NetworkMsg.class

            desc ""
            
			params {

				column {
					name "securityGroupUuid"
					enclosedIn ""
					desc "安全组UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "l3NetworkUuid"
					enclosedIn ""
					desc "三层网络UUID"
					location "url"
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
            clz APIAttachSecurityGroupToL3NetworkEvent.class
        }
    }
}