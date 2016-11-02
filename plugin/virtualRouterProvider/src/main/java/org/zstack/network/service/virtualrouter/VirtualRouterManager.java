package org.zstack.network.service.virtualrouter;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.service.NetworkServiceProviderInventory;
import org.zstack.header.network.service.NetworkServiceType;

import java.util.List;

public interface VirtualRouterManager {
	NetworkServiceProviderInventory getVirtualRouterProvider(); 

	VirtualRouterHypervisorBackend getHypervisorBackend(HypervisorType hypervisorType);
	
	String buildUrl(String mgmtNicIp, String subPath);

	List<String> selectL3NetworksNeedingSpecificNetworkService(List<String> candidate, NetworkServiceType nsType);

    boolean isL3NetworkNeedingNetworkServiceByVirtualRouter(String l3Uuid, String nsType);

    void acquireVirtualRouterVm(VirtualRouterStruct struct, ReturnValueCompletion<VirtualRouterVmInventory> completion);

    VirtualRouterVmInventory getVirtualRouterVm(L3NetworkInventory l3Nw);

    boolean isVirtualRouterRunningForL3Network(String l3Uuid);

    long countVirtualRouterRunningForL3Network(String l3Uuid);

    boolean isVirtualRouterForL3Network(String l3Uuid);

    List<Flow> getPostCreateFlows();

    List<Flow> getPostStartFlows();

    List<Flow> getPostRebootFlows();

    List<Flow> getPostStopFlows();

    List<Flow> getPostMigrateFlows();

    List<Flow> getPostDestroyFlows();

    FlowChain getReconnectFlowChain();

    int getParallelismDegree(String vrUuid);
}
