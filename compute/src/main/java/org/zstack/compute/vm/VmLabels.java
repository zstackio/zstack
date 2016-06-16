package org.zstack.compute.vm;

import org.zstack.core.logging.LogLabel;

/**
 * Created by xing5 on 2016/6/16.
 */
public class VmLabels {
    @LogLabel(messages = {
            "en_US = there a VM not managed by us found on the host[UUID:{0}], VM UUID:{1}",
            "zh_CN = 在物理机[UUID:{0}]上发现了一个数据里面没有记录的虚拟机[UUID:{1}]，请立即手动清理"
    })
    public static final String STRANGER_VM = "vm.strangers.found";
}
