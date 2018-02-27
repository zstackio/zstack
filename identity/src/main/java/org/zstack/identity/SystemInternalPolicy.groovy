package org.zstack.identity

import org.zstack.header.identity.AccountConstant
import org.zstack.header.identity.InternalPolicy
import org.zstack.header.identity.PolicyInventory
import org.zstack.header.identity.rbac.RBACInfo
import org.zstack.identity.rbac.InternalPolicyDefiner

class SystemInternalPolicy implements InternalPolicy {
    @Override
    List<PolicyInventory> getPolices() {
        return InternalPolicyDefiner.New {
            policy {
                name = "system-internal-policy"

                statement {
                    effect = AccountConstant.StatementEffect.Deny

                    RBACInfo.infos.each { info ->
                        info.adminOnlyAPIs.each { action(it) }
                    }
                }

                statement {
                    name = "normal-account-allowed-apis"
                    effect = AccountConstant.StatementEffect.Allow

                    RBACInfo.infos.each { info ->
                        info.normalAPIs.each { action(it) }
                    }

                    action("org.zstack.appliancevm.*")
                    action("org.zstack.header.console.APIRequestConsoleAccessMsg")
                    action("org.zstack.network.service.eip.*")
                    action("org.zstack.header.identity.*(?<!APICreateAccountMsg|APIUpdateAccountMsg|APIShareResourceMsg" +
                            "|APIRevokeResourceSharingMsg|APIUpdateQuotaMsg|APIQueryAccountMsg" +
                            "|APIQuerySharedResourceMsg|APIChangeResourceOwnerMsg|APIAttachRoleToAccountMsg|APIDetachRoleFromAccountMsg)")
                    action("org.zstack.header.network.l3.*")
                    action("org.zstack.network.service.lb.*")
                    action("org.zstack.header.longjob.*")
                    action("org.zstack.network.service.portforwarding.*")
                    action("org.zstack.query.APIBatchQueryMsg")
                    action("org.zstack.network.securitygroup.*")
                    action("org.zstack.header.tag.*")
                    action("org.zstack.network.service.vip.*")
                    action("org.zstack.network.service.virtualrouter.(?!APICreateVirtualRouterOfferingMsg).*")
                    action("org.zstack.header.vm.*")
                    action("org.zstack.header.storage.snapshot.*")
                    action("org.zstack.header.volume.*")
                    action("org.zstack.network.l2.vxlan.vxlanNetwork.APICreateL2VxlanNetworkMsg")
                    action("org.zstack.network.l2.vxlan.vxlanNetwork.APIQueryL2VxlanNetworkMsg")
                    action("org.zstack.header.affinitygroup.*")
                    action("org.zstack.billing.APICalculateAccountSpendingMsg")
                    action("org.zstack.ha.*")
                    action("org.zstack.header.image.APIGetImageQgaMsg")
                    action("org.zstack.header.image.APISetImageQgaMsg")
                    action("org.zstack.ipsec.*")
                    action("org.zstack.monitoring.*")
                    action("org.zstack.prometheus.(?!APIPrometheusQueryPassThroughMsg).*")
                    action("org.zstack.scheduler.*")
                    action("org.zstack.header.vipQos.*")
                    action("org.zstack.mevoco.APIQueryShareableVolumeVmInstanceRefMsg")
                    action("org.zstack.vpc.*")
                    action("org.zstack.vrouterRoute.*")
                    action("org.zstack.header.core.progress.*")
                    action("org.zstack.header.vo.APIGetResourceNamesMsg")
                }

                statement {
                    name = "give-admin-all-apis"
                    effect = AccountConstant.StatementEffect.Allow

                    action("/**")

                    principal("${AccountConstant.PRINCIPAL_ACCOUNT}:${AccountConstant.INITIAL_SYSTEM_ADMIN_UUID}")
                }
            }
        }
    }
}
