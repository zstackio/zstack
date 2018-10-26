package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.*;
import org.zstack.header.vm.*;
import org.zstack.network.l3.L3NetworkManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmAllocateNicForStartingVmFlow implements Flow {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected L3NetworkManager l3nm;
    @Autowired
    private PluginRegistry pluginRgty;

    private static CLogger logger = Utils.getLogger(VmAllocateNicForStartingVmFlow.class);

    @Override
    public void run(final FlowTrigger trigger, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        final VmInstanceInventory vm = spec.getVmInventory();

        if (!VmInstanceConstant.USER_VM_TYPE.equals(vm.getType())) {
            trigger.next();
            return;
        }

        final List<VmNicInventory> nicsNeedNewIp = new ArrayList<VmNicInventory>(vm.getVmNics().size());
        final List<VmNicInventory> usedIpNics = new ArrayList<VmNicInventory>();
        for (VmNicInventory nic : vm.getVmNics()) {
            if (nic.getUsedIpUuid() == null) {
                nicsNeedNewIp.add(nic);
            } else {
                usedIpNics.add(nic);
            }
        }

        // normally, the usedIpUuid of VmNicVO will be set to NULL after an IP range is deleted.
        // however, ill database foreign keys (developers' fault) may cause usedIpUuid not to
        // be cleaned; so in addition to check NULL usedIpUuid, we double check if IP range for every
        // nic is still alive.
        if (!usedIpNics.isEmpty()) {
            nicsNeedNewIp.addAll(findNicsNeedNewIps(usedIpNics));
        }

        if (nicsNeedNewIp.isEmpty()) {
            trigger.next();
            return;
        }

        final Map<String, String> vmStaticIps = new StaticIpOperator().getStaticIpbyVmUuid(vm.getUuid());
        List<AllocateIpMsg> amsgs = CollectionUtils.transformToList(nicsNeedNewIp, new Function<AllocateIpMsg, VmNicInventory>() {
            @Override
            public AllocateIpMsg call(VmNicInventory arg) {
                AllocateIpMsg msg = new AllocateIpMsg();
                msg.setL3NetworkUuid(arg.getL3NetworkUuid());
                msg.setAllocateStrategy(spec.getIpAllocatorStrategy());

                String staticIp = vmStaticIps.get(arg.getL3NetworkUuid());
                if (staticIp != null) {
                    msg.setRequiredIp(staticIp);
                } else {
                    l3nm.updateIpAllocationMsg(msg, arg.getMac());
                }
                bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, arg.getL3NetworkUuid());
                return msg;
            }
        });


        final List<UsedIpInventory> allocatedIPs = new ArrayList<UsedIpInventory>();
        data.put(VmAllocateNicForStartingVmFlow.class, allocatedIPs);

        bus.send(amsgs, new CloudBusListCallBack(trigger) {
            @Override
            public void run(List<MessageReply> replies) {
                for (MessageReply reply : replies) {
                    if (!reply.isSuccess()) {
                        trigger.fail(reply.getError());
                        return;
                    }

                    final AllocateIpReply ar = reply.castReply();
                    final UsedIpInventory ip = ar.getIpInventory();

                    String nicUuid = CollectionUtils.find(nicsNeedNewIp, new Function<String, VmNicInventory>() {
                        @Override
                        public String call(VmNicInventory arg) {
                            return arg.getL3NetworkUuid().equals(ip.getL3NetworkUuid()) ? arg.getUuid() : null;
                        }
                    });

                    for (VmNicExtensionPoint ext : pluginRgty.getExtensionList(VmNicExtensionPoint.class)) {
                        ext.afterAddIpAddress(nicUuid, ip.getUuid());
                    }
                    allocatedIPs.add(UsedIpInventory.valueOf(dbf.findByUuid(ip.getUuid(), UsedIpVO.class)));
                }

                VmInstanceVO vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
                spec.setVmInventory(VmInstanceInventory.valueOf(vmvo));
                spec.setDestNics(spec.getVmInventory().getVmNics());
                trigger.next();
            }
        });
    }

    @Transactional(readOnly = true)
    private List<VmNicInventory> findNicsNeedNewIps(List<VmNicInventory> nics) {
        List<String> usedIpUuids = nics.stream().map(VmNicInventory::getUsedIpUuid).collect(Collectors.toList());

        String sql = "select nic.uuid from VmNicVO nic, UsedIpVO ip, IpRangeVO r where nic.usedIpUuid = ip.uuid and ip.ipRangeUuid = r.uuid and ip.uuid in (:uuids)";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("uuids", usedIpUuids);
        List<String> nicUuids = q.getResultList();

        List<VmNicInventory> needIps = new ArrayList<VmNicInventory>();
        for (VmNicInventory nic : nics) {
            if (!nicUuids.contains(nic.getUuid())) {
                needIps.add(nic);
            }
        }

        return needIps;
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        List<UsedIpInventory> allocatedIps = (List<UsedIpInventory>) data.get(VmAllocateNicForStartingVmFlow.class);
        new While<>(allocatedIps).all((ip, cmpl) -> {
            ReturnIpMsg rmsg = new ReturnIpMsg();
            rmsg.setL3NetworkUuid(ip.getL3NetworkUuid());
            rmsg.setUsedIpUuid(ip.getUuid());
            bus.makeTargetServiceIdByResourceUuid(rmsg, L3NetworkConstant.SERVICE_ID, ip.getL3NetworkUuid());
            bus.send(rmsg, new CloudBusCallBack(cmpl) {
                @Override
                public void run(MessageReply reply) {
                    for (VmNicExtensionPoint ext : pluginRgty.getExtensionList(VmNicExtensionPoint.class)) {
                        ext.afterAddIpAddress(ip.getVmNicUuid(), ip.getUuid());
                    }
                    cmpl.done();
                }
            });
        }).run(new NoErrorCompletion(trigger) {
            @Override
            public void done() {
                trigger.rollback();
            }
        });
    }
}
