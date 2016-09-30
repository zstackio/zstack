package org.zstack.appliancevm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
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
import org.zstack.identity.AccountManager;
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
    private AccountManager acntMgr;

    private UsedIpInventory acquireIp(String l3NetworkUuid, String stratgey) {
        AllocateIpMsg msg = new AllocateIpMsg();
        msg.setL3NetworkUuid(l3NetworkUuid);
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

        if (nicSpec.getIp() == null) {
            String strategy = nicSpec.getAllocatorStrategy() == null ? L3NetworkConstant.RANDOM_IP_ALLOCATOR_STRATEGY : nicSpec.getAllocatorStrategy();
            UsedIpInventory ip = acquireIp(nicSpec.getL3NetworkUuid(), strategy);
            inv.setGateway(ip.getGateway());
            inv.setIp(ip.getIp());
            inv.setNetmask(ip.getNetmask());
            inv.setUsedIpUuid(ip.getUuid());
        } else {
            inv.setGateway(nicSpec.getGateway());
            inv.setIp(nicSpec.getIp());
            inv.setNetmask(nicSpec.getNetmask());
            inv.setUsedIpUuid(null);
            if (nicSpec.getMac() != null) {
                inv.setMac(nicSpec.getMac());
            }
        }

        deviceId[0] ++;
        return inv;
    }

    @Transactional
    private void persistNicInDb(List<VmNicInventory> nics) {
        dbf.entityForTranscationCallback(TransactionalCallback.Operation.PERSIST, VmNicVO.class);
        for (VmNicInventory nic : nics) {
            VmNicVO nvo = new VmNicVO();
            nvo.setUuid(nic.getUuid());
            nvo.setDeviceId(nic.getDeviceId());
            nvo.setIp(nic.getIp());
            nvo.setL3NetworkUuid(nic.getL3NetworkUuid());
            nvo.setMac(nic.getMac());
            nvo.setUsedIpUuid(nic.getUsedIpUuid());
            nvo.setGateway(nic.getGateway());
            nvo.setNetmask(nic.getNetmask());
            nvo.setVmInstanceUuid(nic.getVmInstanceUuid());
            nvo.setMetaData(nic.getMetaData());
            nvo.setInternalName(nic.getInternalName());
            dbf.getEntityManager().persist(nvo);
        }
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

        persistNicInDb(spec.getDestNics());
        String acntUuid = acntMgr.getOwnerAccountUuidOfResource(spec.getVmInventory().getUuid());
        for (VmNicInventory nic : spec.getDestNics()) {
            acntMgr.createAccountResourceRef(acntUuid, nic.getUuid(), VmNicVO.class);
        }

        ApplianceVmVO apvm = dbf.findByUuid(spec.getVmInventory().getUuid(), ApplianceVmVO.class);
        apvm.setManagementNetworkUuid(mgmtNic.getL3NetworkUuid());
        dbf.update(apvm);
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
