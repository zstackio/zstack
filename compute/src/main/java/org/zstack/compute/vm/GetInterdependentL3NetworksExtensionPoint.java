package org.zstack.compute.vm;

import org.zstack.header.network.l3.L3NetworkVO;

import java.util.List;

/**
 * Created by kayo on 2018/10/29.
 */
public interface GetInterdependentL3NetworksExtensionPoint {
    List<L3NetworkVO> afterFilterByImage(List<L3NetworkVO> l3s, List<String> bsUuids, String imageUuid);
}
