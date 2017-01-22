package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode

doc {

    title "云主机控制台地址"

    ref {
        name "error"
        path "org.zstack.header.vm.APIGetVmConsoleAddressReply.error"
        desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", false
        type "ErrorCode"
        since "0.6"
        clz ErrorCode.class
    }
    field {
        name "hostIp"
        desc "云主机运行物理机IP"
        type "String"
        since "0.6"
    }
    field {
        name "port"
        desc "云主机控制台端口"
        type "int"
        since "0.6"
    }
    field {
        name "protocol"
        desc "云主机控制台协议，vnc或spice"
        type "String"
        since "0.6"
    }
    field {
        name "success"
        desc "操作是否成功"
        type "boolean"
        since "0.6"
    }
    ref {
        name "error"
        path "org.zstack.header.vm.APIGetVmConsoleAddressReply.error"
        desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
        type "ErrorCode"
        since "0.6"
        clz ErrorCode.class
    }
}
