package org.zstack.network.service.flat;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.L2NetworkVO_;
import org.zstack.kvm.KVMSystemTags;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;

/**
 * Created by boce.wang on 03/12/2024.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class BridgeVlanIdFinder {
    @Autowired
    private DatabaseFacade dbf;

    public String findByL2Uuid(String l2Uuid) {
        return findByL2Uuid(l2Uuid, true);
    }

    public String findByL2Uuid(String l2Uuid, boolean exceptionOnNotFound) {
        Map<String, String> bridgesVlan = findByL2Uuids(asList(l2Uuid));
        String bridge = KVMSystemTags.L2_BRIDGE_NAME.getTokenByResourceUuid(l2Uuid, KVMSystemTags.L2_BRIDGE_NAME_TOKEN);
        if (bridgesVlan.get(bridge) == null && exceptionOnNotFound) {
            throw new CloudRuntimeException(String.format("cannot find L2 bridge vlan id for the L2 network[uuid:%s]",
                    l2Uuid));
        }
        return bridgesVlan.get(bridge);
    }

    @Transactional(readOnly = true)
    public Map<String, String> findByL2Uuids(Collection<String> l2Uuids) {
        if (l2Uuids == null || l2Uuids.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, String> bridgesVlan = new HashMap<>();
        List<L2NetworkVO> l2Vos = Q.New(L2NetworkVO.class).in(L2NetworkVO_.uuid, asList(l2Uuids)).list();
        l2Vos.forEach(l2 -> {
            String bridge = KVMSystemTags.L2_BRIDGE_NAME.getTokenByResourceUuid(l2.getUuid(), KVMSystemTags.L2_BRIDGE_NAME_TOKEN);
            if (bridge != null && l2.getVirtualNetworkId() != null && !l2.getVirtualNetworkId().equals(0)) {
                // ugly if condition due to history vlan usage which embed in the legacy bridge name
                if (!bridge.contains(l2.getVirtualNetworkId().toString())) {
                    String vlanId = "";
                    if (l2.getType().equals(L2NetworkConstant.L2_VLAN_NETWORK_TYPE)) {
                        vlanId = "vlan" + l2.getVirtualNetworkId();
                    } else if (l2.getType().equals(L2NetworkConstant.VXLAN_NETWORK_POOL_TYPE) ||
                            l2.getType().equals(L2NetworkConstant.VXLAN_NETWORK_TYPE)) {
                        vlanId = "vxlan" + l2.getVirtualNetworkId();
                    }
                    if (!vlanId.isEmpty()) {
                        bridgesVlan.put(bridge, vlanId);
                    }
                }
            }
        });
        return bridgesVlan;
    }
}
