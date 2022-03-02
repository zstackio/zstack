package org.zstack.network.securitygroup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.*;
import org.zstack.network.service.AbstractNetworkServiceExtension;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import javax.persistence.Query;

/**
 */
public class SecurityGroupNetworkServiceExtension extends AbstractNetworkServiceExtension {
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

    private void syncSystemTagToVmNicSecurityGroup(String vmUuid) {
        List<String> tags = VmSystemTags.L3_NETWORK_SECURITY_GROUP_UUIDS_REF.getTags(vmUuid);
        List<VmNicSecurityGroupRefVO> refVOS = new ArrayList<>();
        for (String tag : tags) {
            Map<String, String> tokens = VmSystemTags.L3_NETWORK_SECURITY_GROUP_UUIDS_REF.getTokensByTag(tag);
            String l3Uuid = tokens.get(VmSystemTags.L3_UUID_TOKEN);
            List<String> securityGroupUuids = Arrays.asList(tokens.get(VmSystemTags.SECURITY_GROUP_UUIDS_TOKEN).split(","));

            String vmNicUuid = Q.New(VmNicVO.class)
                    .eq(VmNicVO_.l3NetworkUuid, l3Uuid)
                    .eq(VmNicVO_.vmInstanceUuid, vmUuid)
                    .select(VmNicVO_.uuid)
                    .findValue();
            for (String securityGroupUuid : securityGroupUuids) {
                if (!Q.New(VmNicSecurityGroupRefVO.class)
                        .eq(VmNicSecurityGroupRefVO_.vmNicUuid, vmNicUuid)
                        .eq(VmNicSecurityGroupRefVO_.securityGroupUuid, securityGroupUuid)
                        .eq(VmNicSecurityGroupRefVO_.vmNicUuid, vmUuid)
                        .isExists()) {
                    VmNicSecurityGroupRefVO refVO = new VmNicSecurityGroupRefVO();
                    refVO.setUuid(Platform.getUuid());
                    refVO.setSecurityGroupUuid(securityGroupUuid);
                    refVO.setVmInstanceUuid(vmUuid);
                    refVO.setVmNicUuid(vmNicUuid);
                    refVOS.add(refVO);
                }
            }
        }

        dbf.persistCollection(refVOS);
        VmSystemTags.L3_NETWORK_SECURITY_GROUP_UUIDS_REF.delete(vmUuid);
    }


    @Override
    public void applyNetworkService(VmInstanceSpec servedVm, Map<String, Object> data, final Completion completion) {
        syncSystemTagToVmNicSecurityGroup(servedVm.getVmInventory().getUuid());

        Map<NetworkServiceProviderType, List<L3NetworkInventory>> map = getNetworkServiceProviderMap(SecurityGroupProviderFactory.networkServiceType,
                VmNicSpec.getL3NetworkInventoryOfSpec(servedVm.getL3Networks()));
        if (map.isEmpty()) {
            completion.success();
            return;
        }

        RefreshSecurityGroupRulesOnVmMsg msg = new RefreshSecurityGroupRulesOnVmMsg();
        msg.setVmInstanceUuid(servedVm.getVmInventory().getUuid());
        msg.setHostUuid(servedVm.getDestHost().getUuid());
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
    public void releaseNetworkService(final VmInstanceSpec servedVm, Map<String, Object> data, final NoErrorCompletion completion) {
        RefreshSecurityGroupRulesOnVmMsg msg = new RefreshSecurityGroupRulesOnVmMsg();
        msg.setVmInstanceUuid(servedVm.getVmInventory().getUuid());
        msg.setHostUuid(servedVm.getDestHost().getUuid());
        msg.setDeleteAllRules(true);
        List<String> uuids = new ArrayList<String>();
        for (VmNicInventory nic: servedVm.getDestNics()) {
            uuids.add(nic.getUuid());
        }
        msg.setNicUuids(uuids);

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
}
