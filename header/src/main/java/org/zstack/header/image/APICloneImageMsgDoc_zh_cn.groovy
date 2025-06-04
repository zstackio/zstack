package org.zstack.header.image

import org.zstack.header.image.APICloneImageEvent

doc {
    title "CloneImage"

    category "image"

    desc """克隆镜像"""

    rest {
        request {
			url "POST /v1/image/clone/{imageUuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APICloneImageMsg.class

            desc """"""
            
			params {

				column {
					name "imageUuid"
					enclosedIn "params"
					desc "镜像UUID"
					location "url"
					type "String"
					optional false
					since "5.3.36"
				}
				column {
					name "imageGroupUuid"
					enclosedIn "params"
					desc "镜像组UUID"
					location "body"
					type "String"
					optional false
					since "5.3.36"
				}
				column {
					name "strategy"
					enclosedIn "params"
					desc "克隆策略"
					location "body"
					type "String"
					optional true
					since "5.3.36"
					values ("DatabaseOnly")
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
            clz APICloneImageEvent.class
        }
    }
}