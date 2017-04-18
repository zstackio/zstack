package org.zstack.header.console

doc {
    title "ReconnectConsoleProxyAgent"

    category "console"

    desc "重连控制台代理Agent"

    rest {
        request {
            url "PUT /v1/consoles/agents"


            header (Authorization: 'OAuth the-session-uuid')

            clz APIReconnectConsoleProxyAgentMsg.class

            desc "重连控制台代理Agent"

            params {

                column {
                    name "agentUuids"
                    enclosedIn "params"
                    desc "控制台代理Agent的UUID"
                    location "body"
                    type "List"
                    optional true
                    since "0.6"

                }
                column {
                    name "systemTags"
                    enclosedIn ""
                    desc "系统标签"
                    location "body"
                    type "List"
                    optional true
                    since "0.6"

                }
                column {
                    name "userTags"
                    enclosedIn ""
                    desc "用户标签"
                    location "body"
                    type "List"
                    optional true
                    since "0.6"

                }
            }
        }

        response {
            clz APIReconnectConsoleProxyAgentEvent.class
        }
    }
}