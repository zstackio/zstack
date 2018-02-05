package org.zstack.header.quota;

/**
 * Created by MaJin on 2017/12/27.
 */
public interface QuotaConstant {
    String VM_TOTAL_NUM = "vm.totalNum";
    String VM_RUNNING_NUM = "vm.num";
    String VM_RUNNING_MEMORY_SIZE = "vm.memorySize";
    String VM_RUNNING_CPU_NUM = "vm.cpuNum";
    String DATA_VOLUME_NUM = "volume.data.num";
    String VOLUME_SIZE = "volume.capacity";
    String SG_NUM = "securityGroup.num";
    String L3_NUM = "l3.num";
    String VOLUME_SNAPSHOT_NUM = "snapshot.volume.num";
    String LOAD_BALANCER_NUM = "loadBalancer.num";
    String EIP_NUM = "eip.num";
    String PF_NUM = "portForwarding.num";
    String IMAGE_NUM = "image.num";
    String IMAGE_SIZE = "image.size";
    String VIP_NUM = "vip.num";
    String VXLAN_NUM = "vxlan.num";
}
