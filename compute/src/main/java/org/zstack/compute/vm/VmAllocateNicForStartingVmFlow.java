package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.*;
import org.zstack.header.vm.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmAllocateNicForStartingVmFlow implements Flow {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ErrorFacade errf;

    @Override
    public void run(final FlowTrigger trigger, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        final VmInstanceInventory vm = spec.getVmInventory();

        if (!VmInstanceConstant.USER_VM_TYPE.equals(vm.getType())) {
            trigger.next();
            return;
        }

        final List<VmNicInventory> nicsNeedNewIp = new ArrayList<VmNicInventory>(vm.getVmNics().size());
        List<String> usedIpUuids = new ArrayList<String>();
        for (VmNicInventory nic : vm.getVmNics()) {
            if (nic.getUsedIpUuid() == null) {
                nicsNeedNewIp.add(nic);
            } else if (VmInstanceConstant.NIC_META_RELEASE_IP_AND_ACQUIRE_NEW.equals(nic.getMetaData())) {
                nicsNeedNewIp.add(nic);
                ReturnIpMsg rmsg = new ReturnIpMsg();
                rmsg.setL3NetworkUuid(nic.getL3NetworkUuid());
                rmsg.setUsedIpUuid(nic.getUsedIpUuid());
                bus.makeTargetServiceIdByResourceUuid(rmsg, L3NetworkConstant.SERVICE_ID, nic.getL3NetworkUuid());
                MessageReply reply = bus.call(rmsg);
                if (!reply.isSuccess()) {
                    throw new OperationFailureException(errf.stringToOperationError(
                            String.format("cannot release old ip[%s] of nic[uuid:%s, vm uuid:%s, l3 uuid:%s], %s",
                                    nic.getIp(), nic.getUuid(), nic.getVmInstanceUuid(), nic.getL3NetworkUuid(), reply.getError())
                    ));
                } else {
                    // clear the NIC_META_RELEASE_IP_AND_ACQUIRE_NEW flag
                    VmNicVO nvo = dbf.findByUuid(nic.getUuid(), VmNicVO.class);
                    nvo.setMetaData(null);
                    dbf.update(nvo);
                }
            } else {
                usedIpUuids.add(nic.getUsedIpUuid());
            }
        }

        // normally, the usedIpUuid of VmNicVO will be set to NULL after an IP range is deleted.
        // however, ill database foreign keys (developers' fault) may cause usedIpUuid not to
        // be cleaned; so in addition to check NULL usedIpUuid, we double check if IP range for every
        // nic is still alive.
        if (!usedIpUuids.isEmpty()) {
            nicsNeedNewIp.addAll(findNicsNeedNewIps(usedIpUuids, vm.getVmNics()));
        }

        if (nicsNeedNewIp.isEmpty()) {
            trigger.next();
            return;
        }

        List<AllocateIpMsg> amsgs = CollectionUtils.transformToList(nicsNeedNewIp, new Function<AllocateIpMsg, VmNicInventory>() {
            @Override
            public AllocateIpMsg call(VmNicInventory arg) {
                AllocateIpMsg msg = new AllocateIpMsg();

                List<Map<String, String>> tokenList = VmSystemTags.STATIC_IP.getTokensOfTagsByResourceUuid(spec.getVmInventory().getUuid());
                for (Map<String, String> tokens : tokenList) {
                    String l3Uuid = tokens.get(VmSystemTags.STATIC_IP_L3_UUID_TOKEN);
                    if (l3Uuid.equals(arg.getL3NetworkUuid())) {
                        msg.setRequiredIp(tokens.get(VmSystemTags.STATIC_IP_TOKEN));
                    }
                }

                msg.setL3NetworkUuid(arg.getL3NetworkUuid());
                msg.setAllocateStrategy(spec.getIpAllocatorStrategy());
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
                    allocatedIPs.add(ip);

                    String nicUuid = CollectionUtils.find(nicsNeedNewIp, new Function<String, VmNicInventory>() {
                        @Override
                        public String call(VmNicInventory arg) {
                            return arg.getL3NetworkUuid().equals(ip.getL3NetworkUuid()) ? arg.getUuid() : null;
                        }
                    });

                    VmNicVO nicvo = dbf.findByUuid(nicUuid, VmNicVO.class);
                    nicvo.setGateway(ip.getGateway());
                    nicvo.setNetmask(ip.getNetmask());
                    nicvo.setIp(ip.getIp());
                    nicvo.setUsedIpUuid(ip.getUuid());
                    dbf.update(nicvo);
                }

                VmInstanceVO vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
                spec.setVmInventory(VmInstanceInventory.valueOf(vmvo));
                spec.setDestNics(spec.getVmInventory().getVmNics());
                trigger.next();
            }
        });
    }

    @Transactional(readOnly = true)
    private List<VmNicInventory> findNicsNeedNewIps(List<String> usedIpUuids, List<VmNicInventory> nics) {
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
        if (allocatedIps != null && !allocatedIps.isEmpty()) {
            List<ReturnIpMsg> rmsgs = CollectionUtils.transformToList(allocatedIps, new Function<ReturnIpMsg, UsedIpInventory>() {
                @Override
                public ReturnIpMsg call(UsedIpInventory arg) {
                    ReturnIpMsg rmsg = new ReturnIpMsg();
                    rmsg.setL3NetworkUuid(arg.getL3NetworkUuid());
                    rmsg.setUsedIpUuid(arg.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(rmsg, L3NetworkConstant.SERVICE_ID, arg.getL3NetworkUuid());
                    return rmsg;
                }
            });

            bus.send(rmsgs);
        }

        trigger.rollback();
    }
}
