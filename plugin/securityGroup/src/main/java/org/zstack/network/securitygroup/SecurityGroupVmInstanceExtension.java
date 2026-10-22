package org.zstack.network.securitygroup;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.identity.AccountResourceRefInventory;
import org.zstack.header.identity.ResourceOwnerPreChangeExtensionPoint;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.vm.*;

import java.util.*;
import java.util.stream.Collectors;

public class SecurityGroupVmInstanceExtension implements VmInstanceCreateExtensionPoint, ResourceOwnerPreChangeExtensionPoint {
    @Autowired
    private SecurityGroupPermissionChecker permissionChecker;

    public static boolean isKvmVm(CreateVmInstanceMsg msg) {
        if (msg.getType() != null && !VmInstanceConstant.USER_VM_TYPE.equals(msg.getType())) {
            return false;
        }
        String hypervisorType = null;
        if (msg.getHostUuid() != null) {
            hypervisorType = Q.New(HostVO.class).eq(HostVO_.uuid, msg.getHostUuid()).select(HostVO_.hypervisorType).findValue();
        } else if (msg.getClusterUuid() != null) {
            hypervisorType = Q.New(ClusterVO.class).eq(ClusterVO_.uuid, msg.getClusterUuid()).select(ClusterVO_.hypervisorType).findValue();
        }
        if (hypervisorType != null) {
            return VmInstanceConstant.KVM_HYPERVISOR_TYPE.equals(hypervisorType);
        }
        if (msg.getL3NetworkSpecs() != null && !msg.getL3NetworkSpecs().isEmpty()) {
            List<String> hypervisorTypes = SQL.New("select distinct cluster.hypervisorType from ClusterVO cluster, L2NetworkClusterRefVO ref, L3NetworkVO l3" +
                            " where cluster.uuid = ref.clusterUuid and ref.l2NetworkUuid = l3.l2NetworkUuid and l3.uuid in (:l3Uuids)", String.class)
                    .param("l3Uuids", VmNicSpec.getL3UuidsOfSpec(msg.getL3NetworkSpecs())).list();
            if (!hypervisorTypes.isEmpty() && !hypervisorTypes.contains(VmInstanceConstant.KVM_HYPERVISOR_TYPE)) {
                return false;
            }
        }
        String format = Q.New(ImageVO.class).eq(ImageVO_.uuid, msg.getImageUuid()).select(ImageVO_.format).findValue();
        return !ImageConstant.VMTX_FORMAT_STRING.equals(format);
    }

    public static Map<String, List<String>> getSecurityGroupsByL3(List<String> systemTags) {
        Map<String, List<String>> groups = new LinkedHashMap<>();
        if (systemTags == null) {
            return groups;
        }
        for (String tag : systemTags) {
            if (!VmSystemTags.L3_NETWORK_SECURITY_GROUP_UUIDS_REF.isMatch(tag)) {
                continue;
            }
            Map<String, String> tokens = VmSystemTags.L3_NETWORK_SECURITY_GROUP_UUIDS_REF.getTokensByTag(tag);
            List<String> uuids = Arrays.stream(tokens.get(VmSystemTags.SECURITY_GROUP_UUIDS_TOKEN).split(","))
                    .filter(uuid -> !uuid.isEmpty()).collect(Collectors.toList());
            groups.computeIfAbsent(tokens.get(VmSystemTags.L3_UUID_TOKEN), key -> new ArrayList<>()).addAll(uuids);
        }
        groups.replaceAll((l3Uuid, uuids) -> uuids.stream().distinct().collect(Collectors.toList()));
        return groups;
    }

    @Override
    public void preCreateVmInstance(CreateVmInstanceMsg msg) {
        if (!isKvmVm(msg)) {
            return;
        }
        Map<String, List<String>> groups = getSecurityGroupsByL3(msg.getSystemTags());
        if (msg.getRequestedSecurityGroupUuids() == null && msg.getInheritedSecurityGroupUuids() == null) {
            msg.setRequestedSecurityGroupUuids(groups);
            msg.setInheritedSecurityGroupUuids(new LinkedHashMap<>());
        }
        permissionChecker.validateCreateSecurityGroupSelection(msg.getAccountUuid(), groups, msg.getRequestedSecurityGroupUuids(), msg.getInheritedSecurityGroupUuids());
    }

    public List<String> getHiddenAdminSecurityGroups(String accountUuid, VmNicVO sourceNic) {
        if (sourceNic == null) {
            return new ArrayList<>();
        }
        List<String> groups = Q.New(VmNicSecurityGroupRefVO.class)
                .eq(VmNicSecurityGroupRefVO_.vmNicUuid, sourceNic.getUuid())
                .eq(VmNicSecurityGroupRefVO_.vmInstanceUuid, sourceNic.getVmInstanceUuid())
                .orderBy(VmNicSecurityGroupRefVO_.priority, SimpleQuery.Od.ASC)
                .select(VmNicSecurityGroupRefVO_.securityGroupUuid).listValues();
        return new ArrayList<>(permissionChecker.getHiddenAdminSecurityGroupUuids(accountUuid, groups));
    }

    @Override
    public void resourceOwnerPreChange(AccountResourceRefInventory ref, String newOwnerUuid) {
        if (VmInstanceVO.class.getSimpleName().equals(ref.getResourceType())) {
            permissionChecker.checkVmOwnerChange(ref.getResourceUuid(), newOwnerUuid);
        }
    }
}
