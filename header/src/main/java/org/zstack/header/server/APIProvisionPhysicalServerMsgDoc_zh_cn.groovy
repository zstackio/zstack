package org.zstack.header.server

import org.zstack.header.server.APIProvisionPhysicalServerEvent

doc {
    title "ProvisionPhysicalServer"

    category "physicalServer"

    desc """在这里填写API描述"""

    rest {
        request {
			url "POST /v1/physical-servers/{serverUuid}/provision"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIProvisionPhysicalServerMsg.class

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
					name "networkUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "5.5.16"
				}
				column {
					name "osImageUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "5.5.16"
				}
				column {
					name "osDistribution"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "5.5.16"
					values ("centos7","rocky9","ubuntu22.04")
				}
				column {
					name "kickstartTemplate"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "provisionNicMac"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "customParams"
					enclosedIn "params"
					desc ""
					location "body"
					type "Map"
					optional true
					since "5.5.16"
				}
				column {
					name "longJobName"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.5.16"
				}
				column {
					name "longJobDescription"
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
            clz APIProvisionPhysicalServerEvent.class
        }
    }
}