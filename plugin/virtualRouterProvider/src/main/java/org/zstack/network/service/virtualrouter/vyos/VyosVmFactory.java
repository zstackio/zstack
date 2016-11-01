package org.zstack.network.service.virtualrouter.vyos;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.ApplianceVm;
import org.zstack.appliancevm.ApplianceVmType;
import org.zstack.appliancevm.ApplianceVmVO;
import org.zstack.core.Platform;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.Component;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.network.NetworkException;
import org.zstack.header.network.l2.APICreateL2NetworkMsg;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkCreateExtensionPoint;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.service.NetworkServiceProviderL2NetworkRefVO;
import org.zstack.header.network.service.NetworkServiceProviderVO;
import org.zstack.header.network.service.NetworkServiceProviderVO_;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.network.service.eip.EipConstant;
import org.zstack.network.service.lb.LoadBalancerConstants;
import org.zstack.network.service.virtualrouter.VirtualRouterApplianceVmFactory;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xing5 on 2016/10/31.
 */
public class VyosVmFactory extends VirtualRouterApplianceVmFactory implements Component,
        PrepareDbInitialValueExtensionPoint, L2NetworkCreateExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VyosVmFactory.class);
    public static ApplianceVmType type = new ApplianceVmType(VyosConstants.VYOS_VM_TYPE);

    @Autowired
    private AnsibleFacade asf;
    @Autowired
    private DatabaseFacade dbf;

    private List<String> vyosPostCreateFlows;
    private List<String> vyosPostStartFlows;
    private List<String> vyosPostRebootFlows;
    private List<String> vyosPostDestroyFlows;
    private List<String> vyosReconnectFlows;
    private FlowChainBuilder postCreateFlowsBuilder;
    private FlowChainBuilder postStartFlowsBuilder;
    private FlowChainBuilder postRebootFlowsBuilder;
    private FlowChainBuilder postDestroyFlowsBuilder;
    private FlowChainBuilder reconnectFlowsBuilder;
    private NetworkServiceProviderVO providerVO;

    private final static List<String> supportedL2NetworkTypes = new ArrayList<>();

    static {
        supportedL2NetworkTypes.add(L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE);
        supportedL2NetworkTypes.add(L2NetworkConstant.L2_VLAN_NETWORK_TYPE);
    }

    public void setVyosPostCreateFlows(List<String> vyosPostCreateFlows) {
        this.vyosPostCreateFlows = vyosPostCreateFlows;
    }

    public void setVyosPostStartFlows(List<String> vyosPostStartFlows) {
        this.vyosPostStartFlows = vyosPostStartFlows;
    }

    public void setVyosPostRebootFlows(List<String> vyosPostRebootFlows) {
        this.vyosPostRebootFlows = vyosPostRebootFlows;
    }

    public void setVyosPostDestroyFlows(List<String> vyosPostDestroyFlows) {
        this.vyosPostDestroyFlows = vyosPostDestroyFlows;
    }

    public void setVyosReconnectFlows(List<String> vyosReconnectFlows) {
        this.vyosReconnectFlows = vyosReconnectFlows;
    }

    public List<Flow> getPostCreateFlows() {
        return postCreateFlowsBuilder.getFlows();
    }

    public List<Flow> getPostStartFlows() {
        return postStartFlowsBuilder.getFlows();
    }

    public List<Flow> getPostRebootFlows() {
        return postRebootFlowsBuilder.getFlows();
    }

    public List<Flow> getPostStopFlows() {
        return null;
    }

    public List<Flow> getPostMigrateFlows() {
        return null;
    }

    public List<Flow> getPostDestroyFlows() {
        return postDestroyFlowsBuilder.getFlows();
    }

    public FlowChain getReconnectFlowChain() {
        return reconnectFlowsBuilder.build();
    }

    @Override
    public ApplianceVmType getApplianceVmType() {
        return type;
    }

    @Override
    public ApplianceVm getSubApplianceVm(ApplianceVmVO apvm) {
        VirtualRouterVmVO vr = dbf.findByUuid(apvm.getUuid(), VirtualRouterVmVO.class);
        return new VyosVm(vr);
    }

    private void buildWorkFlowBuilder() {
        postCreateFlowsBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(vyosPostCreateFlows).construct();
        postStartFlowsBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(vyosPostStartFlows).construct();
        postRebootFlowsBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(vyosPostRebootFlows).construct();
        postDestroyFlowsBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(vyosPostDestroyFlows).construct();
        reconnectFlowsBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(vyosReconnectFlows).construct();
    }


    @Override
    public boolean start() {
        buildWorkFlowBuilder();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void prepareDbInitialValue() {
        SimpleQuery<NetworkServiceProviderVO> query = dbf.createQuery(NetworkServiceProviderVO.class);
        query.add(NetworkServiceProviderVO_.type, Op.EQ, VyosConstants.VYOS_ROUTER_PROVIDER_TYPE);
        providerVO = query.find();
        if (providerVO != null) {
            return;
        }

        NetworkServiceProviderVO vo = new NetworkServiceProviderVO();
        vo.setUuid(Platform.getUuid());
        vo.setName(VyosConstants.VYOS_VM_TYPE);
        vo.setDescription("zstack vyos network service provider");
        vo.getNetworkServiceTypes().add(NetworkServiceType.DHCP.toString());
        vo.getNetworkServiceTypes().add(NetworkServiceType.DNS.toString());
        vo.getNetworkServiceTypes().add(NetworkServiceType.SNAT.toString());
        vo.getNetworkServiceTypes().add(NetworkServiceType.PortForwarding.toString());
        vo.getNetworkServiceTypes().add(EipConstant.EIP_NETWORK_SERVICE_TYPE);
        vo.getNetworkServiceTypes().add(LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING);
        vo.setType(VyosConstants.VYOS_ROUTER_PROVIDER_TYPE);
        providerVO = dbf.persistAndRefresh(vo);
    }

    @Override
    public void beforeCreateL2Network(APICreateL2NetworkMsg msg) throws NetworkException {

    }

    @Override
    public void afterCreateL2Network(L2NetworkInventory l2Network) {
        if (!supportedL2NetworkTypes.contains(l2Network.getType())) {
            return;
        }

        NetworkServiceProviderL2NetworkRefVO ref = new NetworkServiceProviderL2NetworkRefVO();
        ref.setNetworkServiceProviderUuid(providerVO.getUuid());
        ref.setL2NetworkUuid(l2Network.getUuid());
        dbf.persist(ref);
        String info = String.format("successfully attach network service provider[uuid:%s, name:%s, type:%s] to l2network[uuid:%s, name:%s, type:%s]",
                providerVO.getUuid(), providerVO.getName(), providerVO.getType(), l2Network.getUuid(), l2Network.getName(), l2Network.getType());
        logger.debug(info);
    }
}
