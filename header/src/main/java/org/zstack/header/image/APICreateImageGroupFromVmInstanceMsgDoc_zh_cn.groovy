package org.zstack.header.image

import org.zstack.header.image.APICreateImageGroupFromVmInstanceEvent

doc {
    title "CreateImageGroupFromVmInstance"

    category "image"

    desc """从虚拟机创建镜像组"""

    rest {
        request {
			url "POST /v1/images/groups/from/vm-instance"

			header (Authorization: 'OAuth the-session-uuid')

            clz APICreateImageGroupFromVmInstanceMsg.class

            desc """"""
            
			params {

				column {
					name "vmInstanceUuid"
					enclosedIn "params"
					desc "云主机UUID"
					location "body"
					type "String"
					optional false
					since "5.3.36"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "5.3.36"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "5.3.36"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "5.3.36"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "5.3.36"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.3.36"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.3.36"
				}
			}
        }

        response {
            clz APICreateImageGroupFromVmInstanceEvent.class
        }
    }
}