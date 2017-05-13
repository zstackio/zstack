package org.zstack.network.service.eip;

import org.zstack.header.vm.VmNicInventory;

import java.util.List;

/**
 * Created by heathhose on 17-5-13.
 */
public interface GetCandidateVmNicsForEipInVirtualRouterExtensionPoint {
    List<VmNicInventory> getCandidateVmNicsForEipInVirtualRouter(APIGetEipAttachableVmNicsMsg msg, List<VmNicInventory> vmNics);

}
