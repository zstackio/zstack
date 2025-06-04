package org.zstack.header.image

import org.zstack.header.image.APICreateImageGroupFromImageEvent

doc {
    title "CreateImageGroupFromImage"

    category "image"

    desc """从镜像创建镜像组"""

    rest {
        request {
			url "POST /v1/imagegroup/from/image/{rootVolumeTemplateUuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APICreateImageGroupFromImageMsg.class

            desc """"""
            
			params {

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
					name "rootVolumeTemplateUuid"
					enclosedIn "params"
					desc ""
					location "url"
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
					name "dateVolumeTemplateUuids"
					enclosedIn "params"
					desc ""
					location "body"
					type "List"
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
            clz APICreateImageGroupFromImageEvent.class
        }
    }
}