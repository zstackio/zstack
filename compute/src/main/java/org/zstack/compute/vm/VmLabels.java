package org.zstack.compute.vm;

import org.zstack.core.logging.LogLabel;

/**
 * Created by xing5 on 2016/6/16.
 */
public class VmLabels {
    @LogLabel(messages = {
            "en_US = there is a VM not managed by us found on the host[UUID:{0}], VM UUID:{1}",
            "zh_CN = 在物理机[UUID:{0}]上发现了一个数据里面没有记录的虚拟机[UUID:{1}]，请立即手动清理"
    })
    public static final String STRANGER_VM = "vm.strangers.found";

    @LogLabel(messages = {
            "en_US = it's about to allocate CPU/memory from host pools",
            "zh_CN = 准备从物理机资源池中分配CPU、内存资源"
    })
    public static final String VM_START_ALLOCATE_HOST = "vm.start.allocateHost";

    @LogLabel(messages = {
            "en_US = found {0} hosts in clusters that are attached to L2 networks required by this VM",
            "zh_CN = 找到{0}台备选物理机，其所在的集群加载了该虚拟机所需要的二层网络"
    })
    public static final String VM_START_ALLOCATE_HOST_L2_SUCCESS = "vm.start.allocateHost.l2.success";

    @LogLabel(messages = {
            "en_US = unable to find any hosts in clusters that are attached to L2 networks required by this VM," +
                    "check items:\n1) check if the L2 networks from which the VM's L3 networks are created are attached to any clusters",
            "zh_CN = 无法分配物理机。该虚拟机的三层网络(L3 Network)所在的二层网络(L2 Network)没有加载到任何集群(cluster)上"
    })
    public static final String VM_START_ALLOCATE_HOST_L2_FAILURE = "vm.start.allocateHost.l2.failure";

    @LogLabel(messages = {
            "en_US = found {0} hosts with hypervisor type {1} are Enabled and Connected",
            "zh_CN = 找到{0}台已启用、已连接，且虚拟化类型是{1}的备选物理机"
    })
    public static final String VM_START_ALLOCATE_HOST_STATE_HYPERVISOR_SUCCESS = "vm.start.allocateHost.hypervisor.success";

    @LogLabel(messages = {
            "en_US = no Connected hosts found in the {0} candidate hosts",
            "zh_CN = 无法分配物理机。备选的{0}台物理机中无任何物理机连接状态为Connected"
    })
    public static final String VM_START_ALLOCATE_HOST_STATE_HYPERVISOR_FAILURE_NO_CONNECTED_HOST = "vm.start.allocateHost.hypervisor.failure.noConnectedHost";

    @LogLabel(messages = {
            "en_US = no Enabled hosts found in the {0} candidate hosts",
            "zh_CN = 无法分配物理机。备选的{0}台物理机中无任何物理机启用状态为Enabled"
    })
    public static final String VM_START_ALLOCATE_HOST_STATE_HYPERVISOR_FAILURE_NO_ENABLED_HOST = "vm.start.allocateHost.hypervisor.failure.noEnabledHost";

    @LogLabel(messages = {
            "en_US = no Enabled hosts found in the {0} candidate hosts having the hypervisor type {1}",
            "zh_CN = 无法分配物理机。备选的{0}台物理机中无任何物理机的虚拟化类型是虚拟机所需要的{1}"
    })
    public static final String VM_START_ALLOCATE_HOST_STATE_HYPERVISOR_FAILURE_NO_HYPERVISOR = "vm.start.allocateHost.hypervisor.failure.noHypervisor";

    @LogLabel(messages = {
            "en_US = found {0} hosts having enough CPU/memory capacity",
            "zh_CN = 找到{0}台备选物理机能够提供虚拟机需要的CPU和内存"
    })
    public static final String VM_START_ALLOCATE_HOST_STATE_CAPACITY_SUCCESS = "vm.start.allocateHost.capacity.success";

    @LogLabel(messages = {
            "en_US = no hosts having enough CPU({0})",
            "zh_CN = 找到{0}台备选物理机能够提供虚拟机需要的CPU和内存"
    })
    public static final String VM_START_ALLOCATE_HOST_STATE_CAPACITY_FAILURE_NOCPU = "vm.start.allocateHost.capacity.failure.noCpu";
}
