package org.zstack.header.server

import org.zstack.header.server.APIUpdatePhysicalServerEvent

doc {
    title "UpdatePhysicalServer"

    category "physicalServer"

    desc """在这里填写API描述"""

    rest {
        request {
			url "PUT /v1/physical-servers/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIUpdatePhysicalServerMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn "updatePhysicalServer"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "5.5.16"
				}
				column {
					name "name"
					enclosedIn "updatePhysicalServer"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "poolUuid"
					enclosedIn "updatePhysicalServer"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "description"
					enclosedIn "updatePhysicalServer"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "managementIp"
					enclosedIn "updatePhysicalServer"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "architecture"
					enclosedIn "updatePhysicalServer"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.5.16"
					values ("x86_64","aarch64")
				}
				column {
					name "serialNumber"
					enclosedIn "updatePhysicalServer"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "manufacturer"
					enclosedIn "updatePhysicalServer"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "model"
					enclosedIn "updatePhysicalServer"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "oobManagementType"
					enclosedIn "updatePhysicalServer"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.5.16"
					values ("IPMI")
				}
				column {
					name "oobAddress"
					enclosedIn "updatePhysicalServer"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "oobPort"
					enclosedIn "updatePhysicalServer"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "5.5.16"
				}
				column {
					name "oobUsername"
					enclosedIn "updatePhysicalServer"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "oobPassword"
					enclosedIn "updatePhysicalServer"
					desc ""
					location "body"
					type "String"
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
            clz APIUpdatePhysicalServerEvent.class
        }
    }
}