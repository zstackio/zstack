package org.zstack.network.service.flat;

import org.zstack.core.logging.LogLabel;

/**
 * Created by xing5 on 2016/6/18.
 */
public class FlatNetworkLabels {
    @LogLabel(messages = {
            "en_US = unable to delete the network namespace for the L3 network[UUID: {0}, name: {1}] on the" +
                    " KVM host[UUID:{2}]. Cause: {3}",
            "zh_CN = 删除三层网络[UUID:{0}, name:{1}]时，其位于KVM物理机[UUID:{2}]上的namespace删除失败，需要手动清理。后端错误" +
                    "原因是:{3}"
    })
    public static String DELETE_NAMESPACE_FAILURE = "flat.dhcp.delete.namespace.failure";
}
