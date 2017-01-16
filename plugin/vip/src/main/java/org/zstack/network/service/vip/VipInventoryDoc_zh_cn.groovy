package org.zstack.network.service.vip

doc {

    title "虚拟IP清单"

    field {
        name "uuid"
        desc "资源的UUID，唯一标示该资源"
        type "String"
        since "0.6"
    }
    field {
        name "name"
        desc "资源名称"
        type "String"
        since "0.6"
    }
    field {
        name "description"
        desc "资源的详细描述"
        type "String"
        since "0.6"
    }
    field {
        name "l3NetworkUuid"
        desc "三层网络UUID"
        type "String"
        since "0.6"
    }
    field {
        name "ip"
        desc "IPv4类型的IP地址"
        type "String"
        since "0.6"
    }
    field {
        name "state"
        desc "状态（启用，禁用）"
        type "String"
        since "0.6"
    }
    field {
        name "gateway"
        desc "网关"
        type "String"
        since "0.6"
    }
    field {
        name "netmask"
        desc "子网掩码"
        type "String"
        since "0.6"
    }
    field {
        name "serviceProvider"
        desc "提供VIP服务的服务提供者"
        type "String"
        since "0.6"
    }
    field {
        name "peerL3NetworkUuid"
        desc "用于的L3网络UUID"
        type "String"
        since "0.6"
    }
    field {
        name "useFor"
        desc "用途，例如：端口转发"
        type "String"
        since "0.6"
    }
    field {
        name "createDate"
        desc "创建时间"
        type "Timestamp"
        since "0.6"
    }
    field {
        name "lastOpDate"
        desc "最后一次修改时间"
        type "Timestamp"
        since "0.6"
    }
}
