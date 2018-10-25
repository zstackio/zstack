package org.zstack.appliancevm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.TransactionalCallback;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowException;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.*;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.VmNicVO;
import org.zstack.identity.Account;
import org.zstack.network.l3.L3NetworkManager;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    private UsedIpInventory acquireIp(String l3NetworkUuid, String mac, String stratgey) {
        AllocateIpMsg msg = new AllocateIpMsg();
        msg.setL3NetworkUuid(l3NetworkUuid);
        l3nm.updateIpAllocationMsg(msg, mac);
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

        if (nicSpec.getIp() == null) {
            String strategy = nicSpec.getAllocatorStrategy() == null ? L3NetworkConstant.RANDOM_IP_ALLOCATOR_STRATEGY : nicSpec.getAllocatorStrategy();
            UsedIpInventory ip = acquireIp(nicSpec.getL3NetworkUuid(), inv.getMac(), strategy);
            inv.setGateway(ip.getGateway());
            inv.setIp(ip.getIp());
            inv.setNetmask(ip.getNetmask());
            inv.setUsedIpUuid(ip.getUuid());
            inv.setIpVersion(ip.getIpVersion());

        } else {
            inv.setGateway(nicSpec.getGateway());
            inv.setIp(nicSpec.getIp());
            inv.setNetmask(nicSpec.getNetmask());
            inv.setUsedIpUuid(null);
            inv.setIpVersion(IPv6Constants.IPv4); /* TODO, shixin fix when ipv6 router is done*/
            if (nicSpec.getMac() != null) {
                inv.setMac(nicSpec.getMac());
            }
        }

        deviceId[0] ++;
        return inv;
    }

    @Transactional
    private void removeNicFromDb(List<VmNicInventory> nics) {
        dbf.entityForTranscationCallback(TransactionalCallback.Operation.REMOVE, VmNicVO.class);
        List<String> uuids = new ArrayList<String>(nics.size());
        for (VmNicInventory nic : nics) {
            uuids.add(nic.getUuid());
        }

        String sql = "delete from VmNicVO v where v.uuid in (:uuids)";
        Query q = dbf.getEntityManager().createQuery(sql);
        q.setParameter("uuids", uuids);
        q.executeUpdate();
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
                    persist(nvo);
                    if (nic.getUsedIpUuid() != null) {
                        SQL.New(UsedIpVO.class).eq(UsedIpVO_.uuid, nic.getUsedIpUuid()).set(UsedIpVO_.vmNicUuid, nvo.getUuid()).update();
                    }
                });

                ApplianceVmVO apvm = findByUuid(spec.getVmInventory().getUuid(), ApplianceVmVO.class);
                apvm.setManagementNetworkUuid(mgmtNic.getL3NetworkUuid());
                merge(apvm);
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

            removeNicFromDb(nics);
            for (VmNicInventory nic : nics) {
                if (nic.getUsedIpUuid() == null) {
                    continue;
                }

                ReturnIpMsg msg = new ReturnIpMsg();
                msg.setL3NetworkUuid(nic.getL3NetworkUuid());
                msg.setUsedIpUuid(nic.getUsedIpUuid());
                bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, nic.getL3NetworkUuid());
                bus.send(msg);
            }
        } finally {
            chain.rollback();
        }
    }
}
