package org.zstack.network.service.portforwarding;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.AbstractNetworkServiceExtension;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 7:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class PortForwardingExtension extends AbstractNetworkServiceExtension {
    private static final CLogger logger = Utils.getLogger(PortForwardingExtension.class);

    @Autowired
    private PortForwardingManager pfMgr;

    private final String SUCCESS = PortForwardingExtension.class.getName();

    public NetworkServiceType getNetworkServiceType() {
        return NetworkServiceType.PortForwarding;
    }

    protected List<PortForwardingStruct> makePortForwardingStruct(List<VmNicInventory> nics, boolean releaseVmNicInfo,L3NetworkInventory l3) {
        VmNicInventory nic = null;
        for (VmNicInventory inv : nics) {
            if (inv.getL3NetworkUuid().equals(l3.getUuid())) {
                nic = inv;
                break;
            }
        }

        SimpleQuery<PortForwardingRuleVO> q = dbf.createQuery(PortForwardingRuleVO.class);
        q.add(PortForwardingRuleVO_.vmNicUuid, SimpleQuery.Op.EQ, nic.getUuid());
        List<PortForwardingRuleVO> pfvos = q.list();
        if (pfvos.isEmpty()) {
            // having port forwarding service but no rules applied yet
            return new ArrayList<PortForwardingStruct>();
        }

        List<PortForwardingStruct> rules = new ArrayList<PortForwardingStruct>();

        for (PortForwardingRuleVO pfvo : pfvos) {
            VipVO vipvo = dbf.findByUuid(pfvo.getVipUuid(), VipVO.class);

            L3NetworkVO l3vo = dbf.findByUuid(vipvo.getL3NetworkUuid(), L3NetworkVO.class);

            PortForwardingStruct struct = new PortForwardingStruct();
            struct.setRule(PortForwardingRuleInventory.valueOf(pfvo));
            struct.setVip(VipInventory.valueOf(vipvo));
            struct.setGuestIp(nic.getIp());
            struct.setGuestMac(nic.getMac());
            struct.setGuestL3Network(l3);
            struct.setSnatInboundTraffic(PortForwardingGlobalConfig.SNAT_INBOUND_TRAFFIC.value(Boolean.class));
            struct.setVipL3Network(L3NetworkInventory.valueOf(l3vo));
            struct.setReleaseVmNicInfoWhenDetaching(releaseVmNicInfo);
            struct.setReleaseVip(false);
            rules.add(struct);
        }

        return rules;
    }

    @Override
    public void applyNetworkService(VmInstanceSpec spec, Map<String, Object> data, Completion completion) {
        // For new created vm, there is no port forwarding rule
        if (spec.getCurrentVmOperation() == VmInstanceConstant.VmOperation.NewCreate) {
            completion.success();
            return;
        }

        Map<String, List<PortForwardingStruct>> structs = workoutPortForwarding(spec);
        Map<String, List<PortForwardingStruct>> applieds = new HashMap<String, List<PortForwardingStruct>>();
        data.put(SUCCESS, applieds);
        applyNetworkService(structs.entrySet().iterator(), applieds, completion);
    }

    private void applyNetworkService(final Iterator<Map.Entry<String, List<PortForwardingStruct>>> it, final Map<String, List<PortForwardingStruct>> applieds, final Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        Map.Entry<String, List<PortForwardingStruct>> e = it.next();
        applyNetworkService(e.getValue().iterator(), e.getKey(), applieds, new Completion(completion) {
            @Override
            public void success() {
                applyNetworkService(it, applieds, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void applyNetworkService(final Iterator<PortForwardingStruct> it, final String providerType, final Map<String, List<PortForwardingStruct>> applieds, final Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        final PortForwardingStruct struct = it.next();
        pfMgr.attachPortForwardingRule(struct, providerType, new Completion(completion) {
            private void addStructToApplieds() {
                List<PortForwardingStruct> structs = applieds.get(providerType);
                if (structs == null) {
                    structs = new ArrayList<PortForwardingStruct>();
                }
                structs.add(struct);
            }

            @Override
            public void success() {
                addStructToApplieds();
                logger.debug(String.format("successfully applied %s", struct.toString()));
                applyNetworkService(it, providerType, applieds, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    public void releaseNetworkService(final Iterator<Map.Entry<String, List<PortForwardingStruct>>> it, final NoErrorCompletion completion) {
        if (!it.hasNext()) {
            completion.done();
            return;
        }

        Map.Entry<String, List<PortForwardingStruct>> e = it.next();
        releaseNetworkService(e.getValue().iterator(), e.getKey(), new NoErrorCompletion(completion) {
            @Override
            public void done() {
                releaseNetworkService(it, completion);
            }
        });
    }

    public void releaseNetworkService(final Iterator<PortForwardingStruct> it, final String providerType, final NoErrorCompletion completion) {
        if (!it.hasNext()) {
            completion.done();
            return;
        }

        final PortForwardingStruct struct = it.next();
        pfMgr.detachPortForwardingRule(struct, providerType, new Completion(completion) {
            @Override
            public void success() {
                logger.debug(String.format("successfully revoked %s on service provider[%s]", struct.toString(), providerType));
                releaseNetworkService(it, providerType, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("failed to revoke %s on service provider[%s], provider should take care of cleanup", struct.toString(), providerType));
                releaseNetworkService(it, providerType, completion);
            }
        });
    }

    @Override
    public void releaseNetworkService(VmInstanceSpec spec, Map<String, Object> data, NoErrorCompletion completion) {
        Map<String, List<PortForwardingStruct>> structs;
        if (data.containsKey(SUCCESS)) {
            structs = (Map<String, List<PortForwardingStruct>>) data.get(SUCCESS);
        } else {
            structs = workoutPortForwarding(spec);
        }

        releaseNetworkService(structs.entrySet().iterator(), completion);
    }

    private Map<String, List<PortForwardingStruct>> workoutPortForwarding(VmInstanceSpec spec) {
        Map<String, List<PortForwardingStruct>> map = new HashMap<String, List<PortForwardingStruct>>();
        Map<NetworkServiceProviderType, List<L3NetworkInventory>> providerMap = getNetworkServiceProviderMap(NetworkServiceType.PortForwarding, spec.getL3Networks());

        for (Map.Entry<NetworkServiceProviderType, List<L3NetworkInventory>> e : providerMap.entrySet()) {
            NetworkServiceProviderType ptype = e.getKey();
            List<PortForwardingStruct> lst = new ArrayList<PortForwardingStruct>();

            for (L3NetworkInventory l3 : e.getValue()) {
                lst.addAll(makePortForwardingStruct(spec.getDestNics(), spec.getCurrentVmOperation() == VmOperation.Destroy || spec.getCurrentVmOperation() == VmOperation.DetachNic, l3));
            }

            map.put(ptype.toString(), lst);
        }

        return map;
    }
}
