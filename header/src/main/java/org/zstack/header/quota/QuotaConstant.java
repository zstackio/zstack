package org.zstack.header.quota;

import org.zstack.utils.data.SizeUnit;

/**
 * Created by AlanJager on 2017/7/19.
 */
public interface QuotaConstant {
    long QUOTA_VM_TOTAL_NUM = 20;
    long QUOTA_VM_RUNNING_NUM = 20;
    long QUOTA_VM_RUNNING_MEMORY_SIZE = SizeUnit.GIGABYTE.toByte(80);
    long QUOTA_VM_RUNNING_CPU_NUM = 80;

    long QUOTA_DATA_VOLUME_NUM = 40;
    long QUOTA_VOLUME_SIZE = SizeUnit.TERABYTE.toByte(10);

    long QUOTA_SG_NUM = 20;
    long QUOTA_L3_NUM = 20;
    long QUOTA_VOLUME_SNAPSHOT_NUM = 200;
    long QUOTA_LOAD_BALANCER_NUM = 20;
    long QUOTA_EIP_NUM = 20;
    long QUOTA_PF_NUM = 20;
    long QUOTA_IMAGE_NUM = 20;
    long QUOTA_IMAGE_SIZE = SizeUnit.TERABYTE.toByte(10);
    long QUOTA_VIP_NUM = 20;
}
