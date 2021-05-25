package org.zstack.appliancevm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.vm.VmNicManager;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.*;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowException;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l3.*;
import org.zstack.header.vm.*;
import org.zstack.identity.Account;
import org.zstack.network.l3.L3NetworkManager;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.NetworkUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:38 PM
 * To change this template use File | Settings | File Templates.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ApplianceVmAllocateNicFlow implements Flow {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private L3NetworkManager l3nm;
    @Autowired
    private VmNicManager nicManager;

    private UsedIpInventory acquireIp(String l3NetworkUuid, String mac, Integer version, String staticIp, String stratgey, boolean allowDuplicatedAddress) {
        AllocateIpMsg msg = new AllocateIpMsg();
        msg.setL3NetworkUuid(l3NetworkUuid);
        if (staticIp != null) {
            msg.setRequiredIp(staticIp);
        }
        msg.setIpVersion(version);
        msg.setDuplicatedIpAllowed(allowDuplicatedAddress);
        if (version == IPv6Constants.IPv6) {
            l3nm.updateIpAllocationMsg(msg, mac);
        }
        bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, l3NetworkUuid);
        msg.setAllocateStrategy(stratgey);
        MessageReply reply = bus.call(msg);
        if (!reply.isSuccess()) {
            throw new FlowException(reply.getError());
        }

        AllocateIpReply areply = (AllocateIpReply) reply;
        return areply.getIpInventory();
    }

    private VmNicInventory makeNicInventory(VmInstanceSpec vmSpec, ApplianceVmNicSpec nicSpec, int[] deviceId) {
        VmNicInventory inv = new VmNicInventory();
        inv.setUuid(Platform.getUuid());
        inv.setL3NetworkUuid(nicSpec.getL3NetworkUuid());
        inv.setVmInstanceUuid(vmSpec.getVmInventory().getUuid());
        inv.setDeviceId(deviceId[0]);
        inv.setMetaData(nicSpec.getMetaData());
        inv.setInternalName(VmNicVO.generateNicInternalName(vmSpec.getVmInventory().getInternalId(), inv.getDeviceId()));
        inv.setMac(NetworkUtils.generateMacWithDeviceId((short) inv.getDeviceId()));
        inv.setHypervisorType(vmSpec.getVmInventory().getHypervisorType());
        inv.setDriverType(ImagePlatform.valueOf(vmSpec.getVmInventory().getPlatform()).isParaVirtualization() ?
                nicManager.getDefaultPVNicDriver() : nicManager.getDefaultNicDriver());

        L3NetworkVO l3NetworkVO = dbf.findByUuid(nicSpec.getL3NetworkUuid(), L3NetworkVO.class);
        L2NetworkVO l2NetworkVO = dbf.findByUuid(l3NetworkVO.getL2NetworkUuid(), L2NetworkVO.class);
        if (l2NetworkVO.getvSwitchType().equals(L2NetworkConstant.VSWITCH_TYPE_OVS_DPDK)) {
            inv.setType("vDPA");
        } else {
            inv.setType(VmInstanceConstant.VIRTUAL_NIC_TYPE);
        }

        inv.setUsedIps(new ArrayList<>());

        if (nicSpec.getIp() == null) {
            /* for vpc router, code comes here */
            List<Integer> ipVersions = l3NetworkVO.getIpVersions();
            for (Integer version : ipVersions) {
                String strategy = nicSpec.getAllocatorStrategy();
                if (strategy == null) {
                    if (version == IPv6Constants.IPv4) {
                        strategy = L3NetworkConstant.RANDOM_IP_ALLOCATOR_STRATEGY;
                    } else {
                        strategy = L3NetworkConstant.RANDOM_IPV6_ALLOCATOR_STRATEGY;
                    }
                }
                UsedIpInventory ip = acquireIp(nicSpec.getL3NetworkUuid(), inv.getMac(), version, nicSpec.getStaticIp().get(version), strategy, nicSpec.isAllowDuplicatedAddress());
                /* save first ip to nic */
                if (inv.getGateway() == null) {
                    inv.setGateway(ip.getGateway());
                    inv.setIp(ip.getIp());
                    inv.setNetmask(ip.getNetmask());
                    inv.setUsedIpUuid(ip.getUuid());
                    inv.setIpVersion(ip.getIpVersion());
                }
                inv.getUsedIps().add(ip);
            }
        } else {
            /* for virtual router, code comes here */
            inv.setGateway(nicSpec.getGateway());
            inv.setIp(nicSpec.getIp());
            inv.setNetmask(nicSpec.getNetmask());
            inv.setUsedIpUuid(null);
            if (NetworkUtils.isIpv4Address(nicSpec.getIp())) {
                inv.setIpVersion(IPv6Constants.IPv4);
            } else {
                inv.setIpVersion(IPv6Constants.IPv6);
            }
            if (nicSpec.getMac() != null) {
                inv.setMac(nicSpec.getMac());
            }
        }

        deviceId[0] ++;
        return inv;
    }

    @Transactional
    private void removeNicFromDb(List<VmNicInventory> nics) {
        SQL.New(VmNicVO.class).in(VmNicVO_.uuid, nics.stream().map(VmNicInventory::getUuid).collect(Collectors.toList())).delete();
    }

    @Override
    public void run(FlowTrigger chain, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        ApplianceVmSpec aspec = spec.getExtensionData(ApplianceVmConstant.Params.applianceVmSpec.toString(), ApplianceVmSpec.class);
        int[] deviceId = {0};

        VmNicInventory mgmtNic = makeNicInventory(spec, aspec.getManagementNic(), deviceId);
        spec.getDestNics().add(mgmtNic);

        for (ApplianceVmNicSpec nicSpec : aspec.getAdditionalNics()) {
            spec.getDestNics().add(makeNicInventory(spec, nicSpec, deviceId));
        }

        new SQLBatch() {
            @Override
            protected void scripts() {
                String acntUuid = Account.getAccountUuidOfResource(spec.getVmInventory().getUuid());
                spec.getDestNics().forEach(nic -> {
                    VmNicVO nvo = new VmNicVO();
                    nvo.setUuid(nic.getUuid());
                    nvo.setDeviceId(nic.getDeviceId());
                    nvo.setIp(nic.getIp());
                    nvo.setL3NetworkUuid(nic.getL3NetworkUuid());
                    nvo.setMac(nic.getMac());
                    nvo.setHypervisorType(nic.getHypervisorType());
                    nvo.setUsedIpUuid(nic.getUsedIpUuid());
                    nvo.setGateway(nic.getGateway());
                    nvo.setNetmask(nic.getNetmask());
                    nvo.setVmInstanceUuid(nic.getVmInstanceUuid());
                    nvo.setMetaData(nic.getMetaData());
                    nvo.setInternalName(nic.getInternalName());
                    nvo.setAccountUuid(acntUuid);
                    nvo.setIpVersion(nic.getIpVersion());
                    nvo.setDriverType(nic.getDriverType());
                    nvo.setType(nic.getType());
                    persist(nvo);
                    for (UsedIpInventory ip : nic.getUsedIps()) {
                        SQL.New(UsedIpVO.class).eq(UsedIpVO_.uuid, ip.getUuid()).set(UsedIpVO_.vmNicUuid, nvo.getUuid()).update();
                    }
                });
            }
        }.execute();
        chain.next();
    }

    @Override
    public void rollback(FlowRollback chain, Map data) {
        try {
            VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
            List<VmNicInventory> nics = spec.getDestNics();
            if (nics.isEmpty()) {
                return;
            }

            List<ReturnIpMsg> rmsgs = new ArrayList<>();
            for (VmNicInventory nic : nics) {
                if (nic.getUsedIps() == null || nic.getUsedIps().isEmpty()) {
                    continue;
                }

                for (UsedIpInventory ip : nic.getUsedIps()) {
                    ReturnIpMsg msg = new ReturnIpMsg();
                    msg.setL3NetworkUuid(nic.getL3NetworkUuid());
                    msg.setUsedIpUuid(ip.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, nic.getL3NetworkUuid());
                    rmsgs.add(msg);
                }
            }

            if (!rmsgs.isEmpty()) {
                new While<>(rmsgs).each((msg, compl) -> {
                    bus.send(msg, new CloudBusCallBack(compl) {
                        @Override
                        public void run(MessageReply reply) {
                            compl.done();
                        }
                    });
                }).run(new WhileDoneCompletion(null) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        removeNicFromDb(nics);
                    }
                });
            } else {
                removeNicFromDb(nics);
            }
        } finally {
            chain.rollback();
        }
    }
}
