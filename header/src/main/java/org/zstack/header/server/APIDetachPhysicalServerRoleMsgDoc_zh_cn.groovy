package org.zstack.header.server

import org.zstack.header.server.APIDetachPhysicalServerRoleEvent

doc {
    title "DetachPhysicalServerRole"

    category "physicalServer"

    desc """在这里填写API描述"""

    rest {
        request {
			url "DELETE /v1/physical-servers/{serverUuid}/roles/{roleType}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIDetachPhysicalServerRoleMsg.class

            desc """"""
            
			params {

				column {
					name "serverUuid"
					enclosedIn ""
					desc ""
					location "url"
					type "String"
					optional false
					since "5.5.16"
				}
				column {
					name "roleType"
					enclosedIn ""
					desc ""
					location "url"
					type "String"
					optional false
					since "5.5.16"
					values ("KVM_HOST","BAREMETAL_V2","CONTAINER_HOST")
				}
				column {
					name "force"
					enclosedIn ""
					desc ""
					location "body"
					type "boolean"
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
            clz APIDetachPhysicalServerRoleEvent.class
        }
    }
}