package org.zstack.identity

import org.zstack.core.gc.APITriggerGCJobMsg
import org.zstack.header.allocator.APIGetCpuMemoryCapacityMsg
import org.zstack.header.cluster.APICreateClusterMsg
import org.zstack.header.configuration.APICreateInstanceOfferingMsg
import org.zstack.header.host.APIAddHostMsg
import org.zstack.header.identity.AccountConstant
import org.zstack.header.identity.InternalPolicy
import org.zstack.header.identity.PolicyInventory
import org.zstack.header.image.APIAddImageMsg
import org.zstack.header.managementnode.APIQueryManagementNodeMsg
import org.zstack.header.network.l2.APICreateL2NetworkMsg
import org.zstack.header.network.l3.APICreateL3NetworkMsg
import org.zstack.header.network.service.APIGetNetworkServiceProviderMsg
import org.zstack.header.storage.backup.APIAddBackupStorageMsg
import org.zstack.header.storage.primary.APIAddPrimaryStorageMsg
import org.zstack.header.zone.APICreateZoneMsg
import org.zstack.identity.rbac.InternalPolicyDefiner

class SystemInternalPolicy implements InternalPolicy {
    @Override
    List<PolicyInventory> getPolices() {
        return InternalPolicyDefiner.New {
            policy {
                name = "admin-only-apis"
                statement {
                    effect = AccountConstant.StatementEffect.Allow

                    action("${APIAddHostMsg.package.name}.*")
                    action("${APICreateZoneMsg.package.name}.*")
                    action("${APICreateClusterMsg.package.name}.*")
                    action("${APIAddBackupStorageMsg.package.name}.*")
                    action("org.zstack.storage.ceph.backup.*")
                    action("${APICreateInstanceOfferingMsg.package.name}.*")
                    action("${APITriggerGCJobMsg.package.name}.*")
                    action("${APIGetCpuMemoryCapacityMsg.package.name}.*")
                    action("${APIAddImageMsg.package.name}.*")
                    action("org.zstack.kvm.*")
                    action("${APICreateL2NetworkMsg.package.name}.*")
                    action("${APICreateL3NetworkMsg.package.name}.*")
                    action("org.zstack.ldap.*")
                    action("org.zstack.storage.primary.local.*")
                    action("${APIGetNetworkServiceProviderMsg.package.name}.*")
                    action("org.zstack.storage.primary.nfs.*")
                    action("${APIQueryManagementNodeMsg.package.name}.*")
                    action("${APIAddPrimaryStorageMsg.package.name}.*")
                    action("org.zstack.storage.backup.sftp.*")
                    action("org.zstack.storage.primary.smp.*")
                    action("org.zstack.network.l2.vxlan.vxlanNetworkPool.*")
                    action("org.zstack.header.baremetal.*")
                    action("org.zstack.storage.backup.imagestore.*")
                    action("org.zstack.license.*")
                    action("org.zstack.pciDevice.*")
                    action("org.zstack.storage.migration.*")
                    action("org.zstack.usbDevice.*")
                    action("org.zstack.vmware.*")

                    principal("${AccountConstant.PRINCIPAL_ACCOUNT}:${AccountConstant.INITIAL_SYSTEM_ADMIN_UUID}")
                }
            }
        }
    }
}
