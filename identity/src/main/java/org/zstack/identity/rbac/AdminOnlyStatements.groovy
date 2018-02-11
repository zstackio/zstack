package org.zstack.identity.rbac

import org.zstack.core.gc.APITriggerGCJobMsg
import org.zstack.header.allocator.APIGetCpuMemoryCapacityMsg
import org.zstack.header.cluster.APICreateClusterMsg
import org.zstack.header.configuration.APICreateInstanceOfferingMsg
import org.zstack.header.host.APIAddHostMsg
import org.zstack.header.image.APIAddImageMsg
import org.zstack.header.managementnode.APIQueryManagementNodeMsg
import org.zstack.header.network.l2.APICreateL2NetworkMsg
import org.zstack.header.network.l3.APICreateL3NetworkMsg
import org.zstack.header.network.service.APIGetNetworkServiceProviderMsg
import org.zstack.header.storage.backup.APIAddBackupStorageMsg
import org.zstack.header.storage.primary.APIAddPrimaryStorageMsg
import org.zstack.header.zone.APICreateZoneMsg

import java.util.regex.Pattern

class AdminOnlyStatements {
    private Pattern p(String s) {
        return Pattern.compile(s)
    }
    
    List<Pattern> actionStatements =[
            p("org.zstack.header.host.*"),
            p("org.zstack.header.zone.*"),
            p("org.zstack.header.cluster.*"),
            p("org.zstack.header.storage.backup.*"),
            p("org.zstack.storage.ceph.backup.*"),
            p("org.zstack.header.configuration.*"),
            p("org.zstack.core.gc.*"),
            p("org.zstack.header.allocator.*"),
            p("org.zstack.header.image.*"),
            p("org.zstack.kvm.*"),
            p("org.zstack.header.network.l2.*"),
            p("org.zstack.ldap.*"),
            p("org.zstack.storage.primary.local.*"),
            p("org.zstack.header.network.service.*"),
            p("org.zstack.storage.primary.nfs.*"),
            p("org.zstack.header.managementnode.*"),
            p("org.zstack.header.storage.primary.*"),
            p("org.zstack.storage.backup.sftp.*"),
            p("org.zstack.storage.primary.smp.*"),
            p("org.zstack.network.l2.vxlan.vxlanNetworkPool.*"),
            p("org.zstack.header.baremetal.*"),
            p("org.zstack.storage.backup.imagestore.*"),
            p("org.zstack.license.*"),
            p("org.zstack.pciDevice.*"),
            p("org.zstack.storage.migration.*"),
            p("org.zstack.usbDevice.*"),
            p("org.zstack.vmware.*"),
    ]

    List<Pattern> resourceStatements =[

    ]

    List<Pattern> getActionStatements() {
        return actionStatements
    }

    List<Pattern> getResourceStatements() {
        return resourceStatements
    }
}
