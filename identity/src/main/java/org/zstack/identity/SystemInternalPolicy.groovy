package org.zstack.identity

import org.zstack.header.console.APIRequestConsoleAccessMsg
import org.zstack.header.identity.APICreateUserMsg
import org.zstack.header.identity.AccountConstant
import org.zstack.header.identity.InternalPolicy
import org.zstack.header.identity.PolicyInventory
import org.zstack.header.longjob.APISubmitLongJobMsg
import org.zstack.header.network.l3.APICreateL3NetworkMsg
import org.zstack.header.storage.snapshot.APIDeleteVolumeSnapshotMsg
import org.zstack.header.vm.APICreateVmInstanceMsg
import org.zstack.header.volume.APICreateDataVolumeMsg
import org.zstack.identity.rbac.InternalPolicyDefiner

class SystemInternalPolicy implements InternalPolicy {
    @Override
    List<PolicyInventory> getPolices() {
        return InternalPolicyDefiner.New {
            policy {
                name = "system-internal-policy"

                statement {
                    name = "normal-account-denied-apis"
                    effect = AccountConstant.StatementEffect.Deny

                    action("org.zstack.header.identity.APICreateAccountMsg")
                    action("org.zstack.header.identity.APIUpdateAccountMsg")
                    action("org.zstack.header.identity.APIShareResourceMsg")
                    action("org.zstack.header.identity.APIRevokeResourceSharingMsg")
                    action("org.zstack.header.identity.APIUpdateQuotaMsg")
                    action("org.zstack.header.identity.APIQueryAccountMsg")
                    action("org.zstack.header.identity.APIQuerySharedResourceMsg")
                    action("org.zstack.header.identity.APIChangeResourceOwnerMsg")
                    action("org.zstack.network.service.virtualrouter.APICreateVirtualRouterOfferingMsg")
                    action("org.zstack.prometheus.APIPrometheusQueryPassThroughMsg")
                }

                statement {
                    name = "normal-account-allowed-apis"
                    effect = AccountConstant.StatementEffect.Allow

                    action("org.zstack.appliancevm.*")
                    action("${APIRequestConsoleAccessMsg.class.name}")
                    action("org.zstack.network.service.eip.*")
                    action("${APICreateUserMsg.class.package.name}.*")
                    action("${APICreateL3NetworkMsg.class.package.name}.*")
                    action("org.zstack.network.service.lb.*")
                    action("${APISubmitLongJobMsg.class.package.name}.*")
                    action("org.zstack.network.service.portforwarding.*")
                    action("org.zstack.query.APIBatchQueryMsg")
                    action("org.zstack.network.securitygroup.*")
                    action("org.zstack.header.tag.*")
                    action("org.zstack.network.service.vip.*")
                    action("org.zstack.network.service.virtualrouter.*")
                    action("${APICreateVmInstanceMsg.class.package.name}.*")
                    action("${APIDeleteVolumeSnapshotMsg.class.package.name}.*")
                    action("${APICreateDataVolumeMsg.class.package.name}.*")
                    action("org.zstack.network.l2.vxlan.vxlanNetwork.APICreateL2VxlanNetworkMsg")
                    action("org.zstack.network.l2.vxlan.vxlanNetwork.APIQueryL2VxlanNetworkMsg")
                    action("org.zstack.header.affinitygroup.*")
                    action("org.zstack.billing.APICalculateAccountSpendingMsg")
                    action("org.zstack.ha.*")
                    action("org.zstack.header.image.APIGetImageQgaMsg")
                    action("org.zstack.header.image.APISetImageQgaMsg")
                    action("org.zstack.ipsec.*")
                    action("org.zstack.monitoring.*")
                    action("org.zstack.prometheus.*")
                    action("org.zstack.scheduler.*")
                    action("org.zstack.header.vipQos.*")
                    action("org.zstack.mevoco.APIQueryShareableVolumeVmInstanceRefMsg")
                    action("org.zstack.vpc.*")
                    action("org.zstack.vrouterRoute.*")
                }

                statement {
                    name = "give-admin-all-apis"
                    effect = AccountConstant.StatementEffect.Allow

                    action(".*")

                    principal("${AccountConstant.PRINCIPAL_ACCOUNT}:${AccountConstant.INITIAL_SYSTEM_ADMIN_UUID}")
                }
            }
        }
    }
}
