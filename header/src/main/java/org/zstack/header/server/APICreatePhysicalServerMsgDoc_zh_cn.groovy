package org.zstack.header.server

import org.zstack.header.server.APICreatePhysicalServerEvent

doc {
    title "CreatePhysicalServer"

    category "physicalServer"

    desc """在这里填写API描述"""

    rest {
        request {
			url "POST /v1/physical-servers"

			header (Authorization: 'OAuth the-session-uuid')

            clz APICreatePhysicalServerMsg.class

            desc """"""
            
			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "5.5.16"
				}
				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional false
					since "5.5.16"
				}
				column {
					name "poolUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "5.5.16"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "managementIp"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "5.5.16"
				}
				column {
					name "architecture"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.5.16"
					values ("x86_64","aarch64")
				}
				column {
					name "serialNumber"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "manufacturer"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "model"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "oobManagementType"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.5.16"
					values ("IPMI")
				}
				column {
					name "oobAddress"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "oobPort"
					enclosedIn "params"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "5.5.16"
				}
				column {
					name "oobUsername"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "oobPassword"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
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
            clz APICreatePhysicalServerEvent.class
        }
    }
}