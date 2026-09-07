package org.zstack.networksecuritypolicyschedule

import org.zstack.header.errorcode.ErrorCode

doc {
    title "获取安全策略定时计划结果"

    field {
        name "inventories"
        desc "所属资源下的定时计划列表"
        type "List"
        since "5.5.38"
    }
    field {
        name "success"
        desc "操作是否成功"
        type "boolean"
        since "5.5.38"
    }
    ref {
        name "error"
        path "org.zstack.networksecuritypolicyschedule.APIGetNetworkSecurityPolicyScheduleReply.error"
        desc "错误码，操作成功时为null"
        type "ErrorCode"
        since "5.5.38"
        clz ErrorCode.class
    }
}
