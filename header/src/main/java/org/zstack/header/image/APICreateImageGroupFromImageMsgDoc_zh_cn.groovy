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
					since "5.4.0"
				}
				column {
					name "rootVolumeTemplateUuid"
					enclosedIn "params"
					desc ""
					location "url"
					type "String"
					optional false
					since "5.4.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "5.4.0"
				}
				column {
					name "dataVolumeTemplateUuids"
					enclosedIn "params"
					desc "数据盘镜像模板 UUID 列表"
					location "body"
					type "List"
					optional true
					since "5.4.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "5.4.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "5.4.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.4.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.4.0"
				}
			}
        }

        response {
            clz APICreateImageGroupFromImageEvent.class
        }
    }
}