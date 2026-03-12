package org.zstack.network.securitygroup;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.asyncbatch.While;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.Completion;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.VmAfterAttachNicExtensionPoint;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.VmNicSpec;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.network.service.AbstractNetworkServiceExtension;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.Query;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

/**
 */
public class SecurityGroupNetworkServiceExtension extends AbstractNetworkServiceExtension implements VmAfterAttachNicExtensionPoint {
    private static final CLogger logger = Utils.getLogger(SecurityGroupNetworkServiceExtension.class);

    @Autowired
    private CloudBus bus;

    @Override
    public NetworkServiceExtensionPosition getNetworkServiceExtensionPosition() {
        return NetworkServiceExtensionPosition.AFTER_VM_CREATED;
    }

    @Override
    public NetworkServiceType getNetworkServiceType() {
        return SecurityGroupProviderFactory.networkServiceType;
    }

    private List<String> syncSystemTagToVmNicSecurityGroup(String vmUuid) {
        final List<String> sgUuids = new ArrayList<>();
        List<String> tags = VmSystemTags.L3_NETWORK_SECURITY_GROUP_UUIDS_REF.getTags(vmUuid);

        for (String tag : tags) {
            Map<String, String> tokens = VmSystemTags.L3_NETWORK_SECURITY_GROUP_UUIDS_REF.getTokensByTag(tag);
            String l3Uuid = tokens.get(VmSystemTags.L3_UUID_TOKEN);
            List<String> securityGroupUuids = Arrays.asList(tokens.get(VmSystemTags.SECURITY_GROUP_UUIDS_TOKEN).split(","));

            sgUuids.addAll(securityGroupUuids);
            String vmNicUuid = Q.New(VmNicVO.class)
                    .eq(VmNicVO_.l3NetworkUuid, l3Uuid)
                    .eq(VmNicVO_.vmInstanceUuid, vmUuid)
                    .select(VmNicVO_.uuid)
                    .findValue();
            List<VmNicSecurityGroupRefVO> refs = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vmNicUuid).eq(VmNicSecurityGroupRefVO_.vmInstanceUuid, vmUuid).list();
            List<VmNicSecurityGroupRefVO> toCreate = new ArrayList<>();
            for (String sgUuid : securityGroupUuids) {
                refs.stream().filter(ref -> ref.getSecurityGroupUuid().equals(sgUuid)).findAny().orElseGet(() -> {
                    VmNicSecurityGroupRefVO refVO = new VmNicSecurityGroupRefVO();
                    refVO.setUuid(Platform.getUuid());
                    refVO.setSecurityGroupUuid(sgUuid);
                    refVO.setVmInstanceUuid(vmUuid);
                    refVO.setVmNicUuid(vmNicUuid);
                    toCreate.add(refVO);
                    return refVO;
                });
            }
            if (!toCreate.isEmpty()) {
                toCreate.stream().forEach(ref -> {
                    ref.setPriority(refs.size() + toCreate.indexOf(ref) + 1);
                });

                dbf.persistCollection(toCreate);

                if (!Q.New(VmNicSecurityPolicyVO.class).eq(VmNicSecurityPolicyVO_.vmNicUuid, vmNicUuid).isExists()) {
                    VmNicSecurityPolicyVO policyVO = new VmNicSecurityPolicyVO();
                    policyVO.setUuid(Platform.getUuid());
                    policyVO.setVmNicUuid(vmNicUuid);
                    policyVO.setIngressPolicy(VmNicSecurityPolicy.DENY.toString());
                    policyVO.setEgressPolicy(VmNicSecurityPolicy.ALLOW.toString());
                    dbf.persist(policyVO);
                }
            }
        }
        VmSystemTags.L3_NETWORK_SECURITY_GROUP_UUIDS_REF.delete(vmUuid);

        tags = VmSystemTags.SECURITY_GROUP_POLICY.getTags(vmUuid);
        for (String tag : tags) {
            Map<String, String> tokens = VmSystemTags.SECURITY_GROUP_POLICY.getTokensByTag(tag);
            String l3Uuid = tokens.get(VmSystemTags.L3_UUID_TOKEN);
            String ingressPolicy = tokens.get(VmSystemTags.SECURITY_GROUP_INGRESS_POLICY_TOKEN);
            String egressPolicy = tokens.get(VmSystemTags.SECURITY_GROUP_EGRESS_POLICY_TOKEN);

            String vmNicUuid = Q.New(VmNicVO.class)
                    .eq(VmNicVO_.l3NetworkUuid, l3Uuid)
                    .eq(VmNicVO_.vmInstanceUuid, vmUuid)
                    .select(VmNicVO_.uuid)
                    .findValue();

            SQL.New(VmNicSecurityPolicyVO.class).eq(VmNicSecurityPolicyVO_.vmNicUuid, vmNicUuid)
                    .set(VmNicSecurityPolicyVO_.ingressPolicy, ingressPolicy)
                    .set(VmNicSecurityPolicyVO_.egressPolicy, egressPolicy).update();
        }
        VmSystemTags.SECURITY_GROUP_POLICY.delete(vmUuid);


        return sgUuids.stream().distinct().collect(Collectors.toList());
    }


    @Override
    public void applyNetworkService(VmInstanceSpec servedVm, Map<String, Object> data, final Completion completion) {
        syncSystemTagToVmNicSecurityGroup(servedVm.getVmInventory().getUuid());

        Map<NetworkServiceProviderType, List<L3NetworkInventory>> map = getNetworkServiceProviderMap(
                SecurityGroupProviderFactory.networkServiceType,
                VmNicSpec.getL3NetworkInventoryOfSpec(servedVm.getL3Networks())
        );
        if (map.isEmpty()) {
            completion.success();
            return;
        }

        List<VmNicInventory> destNics = servedVm.getDestNics();

        List<String> vmNicUuids = destNics.stream()
                .map(VmNicInventory::getUuid)
                .collect(Collectors.toList());

        List<String> sgUuids = Q.New(VmNicSecurityGroupRefVO.class)
                .in(VmNicSecurityGroupRefVO_.vmNicUuid, vmNicUuids)
                .select(VmNicSecurityGroupRefVO_.securityGroupUuid)
                .listValues();

        if (sgUuids == null || sgUuids.isEmpty()) {
            logger.warn(String.format("No security groups found for vmNics: " + vmNicUuids));
            completion.success();
            return;
        }

        Set<String> allRelatedSgUuids = new HashSet<>(sgUuids);

        List<String> remoteSGUuids = Q.New(SecurityGroupRuleVO.class)
                .in(SecurityGroupRuleVO_.remoteSecurityGroupUuid, sgUuids)
                .select(SecurityGroupRuleVO_.securityGroupUuid)
                .listValues();

        allRelatedSgUuids.addAll(remoteSGUuids);

        Set<String> vmUuidsToRefresh = new HashSet<>();

        vmUuidsToRefresh.add(servedVm.getVmInventory().getUuid());

        List<String> vmUuids = Q.New(VmNicSecurityGroupRefVO.class)
                .in(VmNicSecurityGroupRefVO_.securityGroupUuid, new ArrayList<>(allRelatedSgUuids))
                .select(VmNicSecurityGroupRefVO_.vmInstanceUuid)
                .listValues();

        vmUuidsToRefresh.addAll(vmUuids);

        FlowChain schain = FlowChainBuilder.newSimpleFlowChain().setName(String.format("apply-security-group-to-vm-%s", servedVm.getVmInventory().getUuid()));
        schain.allowEmptyFlow();

        Flow applyToCurrentVmFlow = new Flow() {
            String __name__ = "apply-security-group-rules-to-current-vm";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                logger.debug(String.format("Applying security group rules to current VM[uuid:%s]", servedVm.getVmInventory().getUuid()));

                RefreshSecurityGroupRulesOnVmMsg msg = new RefreshSecurityGroupRulesOnVmMsg();
                msg.setVmInstanceUuid(servedVm.getVmInventory().getUuid());
                msg.setHostUuid(servedVm.getDestHost().getUuid());
                msg.setSgUuids(sgUuids);
                msg.setOperation(servedVm.getCurrentVmOperation());
                bus.makeLocalServiceId(msg, SecurityGroupConstant.SERVICE_ID);

                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            trigger.next();
                        } else {
                            trigger.fail(operr(ORG_ZSTACK_NETWORK_SECURITYGROUP_10122, "Failed to apply security group rules to current VM[uuid:%s]",
                                    servedVm.getVmInventory().getUuid())
                                    .causedBy(reply.getError()));
                        }
                    }
                });
            }

            @Override
            public void rollback(FlowRollback rollback, Map data) {
                logger.debug(String.format("No rollback needed for applying security group rules to current VM[uuid:%s]", servedVm.getVmInventory().getUuid()));
                rollback.rollback();
            }
        };

        schain.then(applyToCurrentVmFlow);

        Flow applyToOtherVmsFlow = new Flow() {
            String __name__ = "apply-security-group-rules-to-other-vms";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                Set<String> otherVmUuids = new HashSet<>(vmUuidsToRefresh);
                otherVmUuids.remove(servedVm.getVmInventory().getUuid());

                if (otherVmUuids.isEmpty()) {
                    trigger.next();
                    return;
                }

                List<ErrorCode> errs = Collections.synchronizedList(new ArrayList<>());

                logger.debug(String.format("Applying security group rules to %d other VMs: %s", otherVmUuids.size(), otherVmUuids));

                new While<>(otherVmUuids)
                        .each((vmUuid, wcompl) -> {
                            RefreshSecurityGroupRulesOnVmMsg msg = new RefreshSecurityGroupRulesOnVmMsg();
                            msg.setVmInstanceUuid(vmUuid);
                            bus.makeTargetServiceIdByResourceUuid(msg, SecurityGroupConstant.SERVICE_ID, vmUuid);

                            bus.send(msg, new CloudBusCallBack(wcompl) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        logger.warn(String.format("Failed to refresh security group rules for VM[uuid:%s]: %s",
                                                vmUuid, reply.getError()));
                                        errs.add(reply.getError());
                                    }
                                    wcompl.done();
                                }
                            });
                        })
                        .run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (errs.isEmpty()) {
                                    trigger.next();
                                } else {
                                    trigger.fail(operr(ORG_ZSTACK_NETWORK_SECURITYGROUP_10123, "Failed to apply security group rules to some VMs"));
                                }
                            }
                        });
            }

            @Override
            public void rollback(FlowRollback rollback, Map data) {
                logger.debug("Rolling back security group application to other VMs... (no-op)");
                rollback.rollback();
            }
        };

        schain.then(applyToOtherVmsFlow);

        schain.error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode err, Map data) {
                completion.fail(err);
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).start();
    }

    @Override
    public void releaseNetworkService(final VmInstanceSpec servedVm, Map<String, Object> data, final NoErrorCompletion completion) {
        if (!Optional.ofNullable(servedVm.getDestHost()).isPresent()){
            completion.done();
            return;
        }
        RefreshSecurityGroupRulesOnVmMsg msg = new RefreshSecurityGroupRulesOnVmMsg();
        msg.setVmInstanceUuid(servedVm.getVmInventory().getUuid());
        msg.setHostUuid(servedVm.getDestHost().getUuid());
        msg.setDeleteAllRules(true);
        List<String> uuids = new ArrayList<String>();
        for (VmNicInventory nic: servedVm.getDestNics()) {
            uuids.add(nic.getUuid());
        }
        msg.setNicUuids(uuids);
        msg.setOperation(servedVm.getCurrentVmOperation());

        bus.makeLocalServiceId(msg, SecurityGroupConstant.SERVICE_ID);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.debug(String.format("failed to remove security group rules for vm[uuid:%s], %s", servedVm.getVmInventory().getUuid(), reply.getError()));
                }
                if (servedVm.getCurrentVmOperation() == VmInstanceConstant.VmOperation.DetachNic) {
                    for (VmNicInventory nic: servedVm.getDestNics()) {
                        deleteVmNicSecurityGroupRef(nic.getUuid());
                    }
                }
                if (servedVm.getCurrentVmOperation() == VmInstanceConstant.VmOperation.ChangeNicNetwork) {
                    for (String nicUuid : uuids) {
                        deleteVmNicSecurityGroupRef(nicUuid);
                    }
                }

                completion.done();
            }
        });
    }

    @Transactional
    private void deleteVmNicSecurityGroupRef(String vmNicUuid){
	    String sql = String.format("delete from %s ref where ref.%s = :id",
			    VmNicSecurityGroupRefVO.class.getSimpleName(), "vmNicUuid");
	    Query query = dbf.getEntityManager().createQuery(sql);
	    query.setParameter("id", vmNicUuid);
	    query.executeUpdate();
    }

    @Override
    public void afterAttachNic(String nicUuid, VmInstanceInventory vmInstanceInventory, Completion completion) {
        List<String> sgUuids = syncSystemTagToVmNicSecurityGroup(vmInstanceInventory.getUuid());
        if (StringUtils.isEmpty(vmInstanceInventory.getHostUuid())) {
            completion.success();
            return;
        }
        RefreshSecurityGroupRulesOnVmMsg msg = new RefreshSecurityGroupRulesOnVmMsg();
        msg.setVmInstanceUuid(vmInstanceInventory.getUuid());
        msg.setHostUuid(vmInstanceInventory.getHostUuid());
        msg.setSgUuids(sgUuids);
        msg.setOperation(VmInstanceConstant.VmOperation.AttachNic);
        bus.makeLocalServiceId(msg, SecurityGroupConstant.SERVICE_ID);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void afterAttachNicRollback(String nicUuid, VmInstanceInventory vmInstanceInventory, NoErrorCompletion completion) {
        logger.debug(String.format("securityGroupNetworkServiceExtension after attach nic starting rollback, hardDelete VmNicSecurityGroupRefVO data"));
        SQL.New(VmNicSecurityGroupRefVO.class)
                .eq(VmNicSecurityGroupRefVO_.vmNicUuid, nicUuid)
                .eq(VmNicSecurityGroupRefVO_.vmNicUuid, vmInstanceInventory.getUuid()).hardDelete();
        completion.done();
    }

    @Override
    public void enableNetworkService(L3NetworkVO l3VO, NetworkServiceProviderType providerType, List<String> systemTags, Completion completion) {
        completion.success();
    }

    @Override
    public void disableNetworkService(L3NetworkVO l3VO, NetworkServiceProviderType providerType, Completion completion) {
        completion.success();
    }
}
