package org.zstack.header.server

import org.zstack.header.server.APIAttachPhysicalServerRoleEvent

doc {
    title "AttachPhysicalServerRole"

    category "physicalServer"

    desc """在这里填写API描述"""

    rest {
        request {
			url "POST /v1/physical-servers/{serverUuid}/roles"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIAttachPhysicalServerRoleMsg.class

            desc """"""
            
			params {

				column {
					name "serverUuid"
					enclosedIn "params"
					desc ""
					location "url"
					type "String"
					optional false
					since "5.5.16"
				}
				column {
					name "roleType"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "5.5.16"
					values ("KVM_HOST","BAREMETAL_V2","CONTAINER_HOST")
				}
				column {
					name "clusterUuid"
					enclosedIn "params"
					desc "集群UUID"
					location "body"
					type "String"
					optional false
					since "5.5.16"
				}
				column {
					name "roleConfig"
					enclosedIn "params"
					desc ""
					location "body"
					type "Map"
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
            clz APIAttachPhysicalServerRoleEvent.class
        }
    }
}