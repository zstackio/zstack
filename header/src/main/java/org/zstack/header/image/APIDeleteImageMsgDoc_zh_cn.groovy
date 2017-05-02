package org.zstack.header.image

doc {
    title "删除镜像(DeleteImage)"

    category "image"

    desc "删除镜像"

    rest {
        request {
			url "DELETE /v1/images/{uuid}"


            header (Authorization: 'OAuth the-session-uuid')

            clz APIDeleteImageMsg.class

            desc ""
            
			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "镜像的UUID，唯一标示该镜像"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "backupStorageUuids"
					enclosedIn "params"
					desc "镜像服务器UUID列表"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "deleteMode"
					enclosedIn "params"
					desc "删除模式(Permissive 或者 Enforcing, 默认 Permissive)"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn "params"
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn "params"
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIDeleteImageEvent.class
        }
    }
}