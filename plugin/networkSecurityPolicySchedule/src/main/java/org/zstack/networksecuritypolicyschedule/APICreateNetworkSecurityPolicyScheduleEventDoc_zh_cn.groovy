package org.zstack.networksecuritypolicyschedule

import org.zstack.header.errorcode.ErrorCode
import org.zstack.networksecuritypolicyschedule.NetworkSecurityPolicyScheduleInventory

doc {
    title "创建安全策略定时计划结果"

    ref {
        name "inventory"
        path "org.zstack.networksecuritypolicyschedule.APICreateNetworkSecurityPolicyScheduleEvent.inventory"
        desc "定时计划清单"
        type "NetworkSecurityPolicyScheduleInventory"
        since "5.5.38"
        clz NetworkSecurityPolicyScheduleInventory.class
    }
    field {
        name "success"
        desc "操作是否成功"
        type "boolean"
        since "5.5.38"
    }
    ref {
        name "error"
        path "org.zstack.networksecuritypolicyschedule.APICreateNetworkSecurityPolicyScheduleEvent.error"
        desc "错误码，操作成功时为null"
        type "ErrorCode"
        since "5.5.38"
        clz ErrorCode.class
    }
}
