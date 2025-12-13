package org.zstack.header.network.service;

import org.zstack.header.core.Completion;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;

import java.util.List;

/**
 * 该接口定义了 SDN 控制器处理 DHCP 服务的方法。
 * 实现该接口的类负责在 SDN 环境中分配、启用和禁用 DHCP 服务。
 + */
public interface SdnControllerDhcp {

    /**
     * 为 L3 网络分配 DHCP 服务并启用
     * @param vo L3 网络 VO 对象
     * @param systemTags 系统标签列表
     * @param completion 操作完成后的回调
     * */

    void allocateDhcpAndEnableDhcp(L3NetworkVO vo, List<String> systemTags, Completion completion);

    /**
     * 启用指定 L3 网络的 DHCP 服务
     * @param l3Min L3 网络索引
     * @param l3Max L3 网络索引
     * @param invs L3 网络清单列表
     * @param sync 是否同步操作
     * @param completion 操作完成后的回调
     * */

    void enableDhcp(long l3Min, long l3Max, List<L3NetworkInventory> invs, Integer ipversion, boolean sync, Completion completion);

    /**
     * 启用指定 L3 网络的 DHCP 服务
     * @param invs L3 网络清单列表
     * @param completion 操作完成后的回调
     * */

    void enableDhcp(List<L3NetworkInventory> invs, Integer ipversion, Completion completion);

    /**
     * 禁用指定 L3 网络的 DHCP 服务
     * @param invs L3 网络清单列表
     * @param ipversion IP 版本
     * @param completion 操作完成后的回调
     * */

    void disableDhcp(List<L3NetworkInventory> invs, Integer ipversion, Completion completion);
}
