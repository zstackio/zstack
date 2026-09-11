package org.zstack.test.integration.networkservice.provider.flat.securitygroup

import org.zstack.core.db.Q
import org.zstack.network.securitygroup.SecurityGroupGlobalConfig
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO_
import org.zstack.sdk.AddVmNicToSecurityGroupAction
import org.zstack.sdk.SecurityGroupInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.flat.FlatNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class VmNicSecurityGroupNumLimitCase extends SubCase {
    EnvSpec env
    Integer originalLimit

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = FlatNetworkServiceEnv.oneHostNoVmEnv()
    }

    @Override
    void clean() {
        try {
            env.delete()
        } finally {
            if (originalLimit != null) {
                SecurityGroupGlobalConfig.VMNIC_SECURITY_GROUP_NUM_LIMIT.updateValue(originalLimit)
            }
        }
    }

    @Override
    void test() {
        env.create {
            originalLimit = SecurityGroupGlobalConfig.VMNIC_SECURITY_GROUP_NUM_LIMIT.value(Integer.class)
            updateGlobalConfig {
                category = SecurityGroupGlobalConfig.CATEGORY
                name = SecurityGroupGlobalConfig.VMNIC_SECURITY_GROUP_NUM_LIMIT.name
                value = "10"
            }

            String l3Uuid = env.inventoryByName("l3").uuid
            def provider = queryNetworkServiceProvider { conditions = ["type=SecurityGroup"] }[0]
            attachNetworkServiceToL3Network {
                l3NetworkUuid = l3Uuid
                networkServices = [(provider.uuid): ["SecurityGroup"]]
            }

            List<SecurityGroupInventory> groups = (1..11).collect { index ->
                createSecurityGroup {
                    name = "limit-sg-${index}"
                    ipVersion = 4
                } as SecurityGroupInventory
            }
            VmInstanceInventory vm = createVmInstance {
                name = "security-group-limit-vm"
                imageUuid = env.inventoryByName("image").uuid
                instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
                l3NetworkUuids = [l3Uuid]
            }
            String nicUuid = vm.vmNics[0].uuid

            groups.take(10).each { group ->
                addVmNicToSecurityGroup {
                    securityGroupUuid = group.uuid
                    vmNicUuids = [nicUuid]
                }
            }
            Set<String> expected = groups.take(10)*.uuid as Set<String>
            Set<String> actual = Q.New(VmNicSecurityGroupRefVO.class)
                    .select(VmNicSecurityGroupRefVO_.securityGroupUuid)
                    .eq(VmNicSecurityGroupRefVO_.vmNicUuid, nicUuid).listValues() as Set<String>
            assert actual == expected : "the first ten security groups must be bound: expected=${expected}, actual=${actual}"

            def result = new AddVmNicToSecurityGroupAction(
                    securityGroupUuid: groups[10].uuid,
                    vmNicUuids: [nicUuid],
                    sessionId: adminSession()).call()
            assert result.error?.globalErrorCode == "ORG_ZSTACK_NETWORK_SECURITYGROUP_10130" :
                    "the eleventh binding must be rejected by the configured limit: error=${result.error}"

            actual = Q.New(VmNicSecurityGroupRefVO.class)
                    .select(VmNicSecurityGroupRefVO_.securityGroupUuid)
                    .eq(VmNicSecurityGroupRefVO_.vmNicUuid, nicUuid).listValues() as Set<String>
            assert actual == expected : "the rejected binding must not change database refs: expected=${expected}, actual=${actual}"
        }
    }
}
