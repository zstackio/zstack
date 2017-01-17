package org.zstack.network.securitygroup

import org.zstack.network.securitygroup.APIGetCandidateVmNicForSecurityGroupReply

doc {
    title "GetCandidateVmNicForSecurityGroup"

    category "securityGroup"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/security-groups/{securityGroupUuid}/vm-instances/candidate-nics"


            header (OAuth: 'the-session-uuid')

            clz APIGetCandidateVmNicForSecurityGroupMsg.class

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
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIGetCandidateVmNicForSecurityGroupReply.class
        }
    }
}