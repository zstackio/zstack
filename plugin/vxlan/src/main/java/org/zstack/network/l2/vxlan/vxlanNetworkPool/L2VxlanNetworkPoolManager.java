package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import java.util.List;

/**
 * Created by weiwang on 10/03/2017.
 */
public interface L2VxlanNetworkPoolManager {
    VniAllocatorStrategy getVniAllocatorStrategy(VniAllocatorType type);

    boolean isVniRangFull(VniRangeVO vo);

    List<Integer> getUsedVniInRange(String vniRangeUuid);
}
