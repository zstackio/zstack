package org.zstack.zcenter.accounts.api

import org.zstack.zcenter.accounts.api.APICreateSessionForZCenterAccountEvent

doc {
	title "CreateSessionForZCenterAccount"

	category "zcenter-accounts"

	desc """为 ZCenter 账户创建会话"""

	rest {
		request {
			url "POST /v1/zcenter/accounts/sessions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateSessionForZCenterAccountMsg.class

			desc """At least one of accountUuid and accountName must be provided, and they cannot be provided at the same time."""

			params {

				column {
					name "accountUuid"
					enclosedIn "params"
					desc "账户UUID"
					location "body"
					type "String"
					optional true
					since "5.1.0"
				}
				column {
					name "accountName"
					enclosedIn "params"
					desc "账户名称"
					location "body"
					type "String"
					optional true
					since "5.1.0"
				}
				column {
					name "source"
					enclosedIn "params"
					desc "账户来源"
					location "body"
					type "String"
					optional true
					since "5.1.0"
					values ("Local","OpenLdap","WindowsAD","CAS","OAuth2","ZCenter")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.1.0"
				}
			}
		}

		response {
			clz APICreateSessionForZCenterAccountEvent.class
		}
	}
}