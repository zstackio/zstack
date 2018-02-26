package org.zstack.identity.rbac

import org.zstack.header.identity.rbac.RBACInfo

import java.util.regex.Pattern

class AdminOnlyStatements {
    private static Pattern p(String s) {
        return Pattern.compile(s)
    }

    static {
        RBACInfo.infos.each { info ->
            info.adminOnlyAPIs.each {
                actionStatements.add(p(it))
            }
        }
    }
    
    static List<Pattern> actionStatements =[
            //p("^org.zstack.header.host.*"),
            p("^org.zstack.header.zone.*"),
            p("^org.zstack.header.cluster.*"),
            p("^org.zstack.header.storage.backup.*"),
            p("^org.zstack.storage.ceph.backup.*"),
            p("^org.zstack.storage.ceph.primary.*"),
            p("^org.zstack.header.configuration.*"),
            p("^org.zstack.core.gc.*"),
            p("^org.zstack.header.allocator.*"),
            p("^org.zstack.header.image.*"),
            p("^org.zstack.kvm.*"),
            p("^org.zstack.header.network.l2.*"),
            p("^org.zstack.ldap.*"),
            p("^org.zstack.storage.primary.local.*"),
            p("^org.zstack.header.network.service.*"),
            p("^org.zstack.storage.primary.nfs.*"),
            p("^org.zstack.header.managementnode.*"),
            p("^org.zstack.header.storage.primary.*"),
            p("^org.zstack.storage.backup.sftp.*"),
            p("^org.zstack.storage.primary.smp.*"),
            p("^org.zstack.network.l2.vxlan.*(?<!APICreateL2VxlanNetworkMsg|APIQueryL2VxlanNetworkMsg)"),
            p("^org.zstack.header.baremetal.*"),
            p("^org.zstack.storage.backup.imagestore.*"),
            p("^org.zstack.license.*"),
            p("^org.zstack.pciDevice.*"),
            p("^org.zstack.storage.migration.*"),
            p("^org.zstack.usbDevice.*"),
            p("^org.zstack.vmware.*"),
            p("^org.zstack.network.service.flat.*"),
            p("^org.zstack.header.identity.(APICreateAccountMsg|APIUpdateAccountMsg|APIShareResourceMsg" +
                    "|APIRevokeResourceSharingMsg|APIUpdateQuotaMsg|APIQueryAccountMsg" +
                    "|APIQuerySharedResourceMsg|APIChangeResourceOwnerMsg)"),
            p("^org.zstack.header.simulator.*"),
            p("^org.zstack.header.core.webhooks.*"),
            p("^org.zstack.core.config.*"),
            p("^org.zstack.header.console.*(?<!APIRequestConsoleAccessMsg)"),
            p("^org.zstack.network.service.virtualrouter.APICreateVirtualRouterOfferingMsg"),
            p("^org.zstack.header.identity.role.api.(APIAttachRoleToAccountMsg|APIDetachRoleFromAccountMsg)"),
    ]

    List<Pattern> resourceStatements =[

    ]

    static List<Pattern> getActionStatements() {
        return actionStatements
    }

    List<Pattern> getResourceStatements() {
        return resourceStatements
    }
}
