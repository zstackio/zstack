package org.zstack.header.image

doc {
    title "更新镜像信息(UpdateImage)"

    category "image"

    desc "更新镜像信息"

    rest {
        request {
			url "PUT /v1/images/{uuid}/actions"


            header (Authorization: 'OAuth the-session-uuid')

            clz APIUpdateImageMsg.class

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
					name "name"
					enclosedIn "params"
					desc "镜像名称"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "description"
					enclosedIn "params"
					desc "镜像的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "guestOsType"
					enclosedIn "params"
					desc "镜像对应的客户机操作系统类型"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "mediaType"
					enclosedIn "params"
					desc "镜像的类型"
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("RootVolumeTemplate","DataVolumeTemplate","ISO")
				}
				column {
					name "format"
					enclosedIn "params"
					desc "镜像的格式"
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("raw","qcow2","iso")
				}
				column {
					name "system"
					enclosedIn "params"
					desc "标识是否为系统镜像"
					location "body"
					type "Boolean"
					optional true
					since "0.6"
					
				}
				column {
					name "platform"
					enclosedIn "params"
					desc "镜像的系统平台"
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("Linux","Windows","Other","Paravirtualization","WindowsVirtio")
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
            clz APIUpdateImageEvent.class
        }
    }
}