package org.zstack.test.integration.identity

import org.zstack.header.network.l3.L3NetworkConstant
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L2NetworkInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PolicyInventory
import org.zstack.sdk.PolicyStatement
import org.zstack.sdk.PolicyStatementEffect
import org.zstack.sdk.PortForwardingRuleInventory
import org.zstack.sdk.SecurityGroupInventory
import org.zstack.sdk.SessionInventory
import org.zstack.sdk.SystemTagInventory
import org.zstack.sdk.UserTagInventory
import org.zstack.sdk.VipInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * @Author: fubang
 * @Date: 2018/6/13
 */
class InvalidDeleteResourceCase extends SubCase{
    EnvSpec env
    SessionInventory testSessionInventory

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            createAccount {
                name = "test"
                password = "password"
            }

            testSessionInventory = logInByAccount {
                accountName = "test"
                password = "password"
            } as SessionInventory

            testL3Network()
            testDiskOffering()
            testPolicy()
            testVolumeSnapshot()
            testDataVolume()
            testPortForwardingRule()
            testSecurityGroup()
        }
    }

    void testL3Network(){
        L2NetworkInventory l2NetworkInventory = env.inventoryByName("l2")

        L3NetworkInventory l3NetworkInventory = createL3Network {
            name = "testL3"
            l2NetworkUuid = l2NetworkInventory.uuid
            type = L3NetworkConstant.L3_BASIC_NETWORK_TYPE
        }

        expect([AssertionError.class]){
            deleteL3Network {
                uuid = l3NetworkInventory.uuid
                sessionId = testSessionInventory.uuid
            }
        }
    }

    void testDiskOffering(){
        DiskOfferingInventory diskOfferingInventory = createDiskOffering {
            name = "testDiskOffering"
            diskSize = SizeUnit.GIGABYTE.toByte(20)
        }

        expect([AssertionError.class]){
            deleteDiskOffering {
                uuid = diskOfferingInventory.uuid
                sessionId = testSessionInventory.uuid
            }
        }
    }

    void testPolicy(){
        PolicyInventory policyInventory = createPolicy {
            name = "id1-policy"
            statements = [new PolicyStatement(
                    name: "allow",
                    effect: PolicyStatementEffect.Allow,
                    actions: [
                            "org.zstack.header.image.APIAddImageMsg",
                            "org.zstack.header.image.APIDeleteImageMsg",
                    ]
            )]
        }

        expect([AssertionError.class]){
            deletePolicy {
                uuid = policyInventory.uuid
                sessionId = testSessionInventory.uuid
            }
        }
    }

    void testVolumeSnapshot(){
        VmInstanceInventory vmInstanceInventory = env.inventoryByName("vm")

        VolumeSnapshotInventory volumeSnapshotInventory = createVolumeSnapshot {
            name = "testVolumeSnapshot"
            volumeUuid = vmInstanceInventory.rootVolumeUuid
        }

        expect([AssertionError.class]){
            deleteVolumeSnapshot {
                uuid = volumeSnapshotInventory.uuid
                sessionId = testSessionInventory.uuid
            }
        }
    }

    void testDataVolume(){
        VolumeInventory volumeInventory = createDataVolume {
            name = 'testDataVolume'
            diskOfferingUuid = env.inventoryByName("diskOffering").uuid
        }

        expect([AssertionError.class]){
            deleteDataVolume {
                uuid = volumeInventory.uuid
                sessionId = testSessionInventory.uuid
            }
        }
    }

    void testPortForwardingRule(){
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3")

        VipInventory vipInventory = createVip {
            name = "vip"
            l3NetworkUuid = pubL3.uuid
        }

        PortForwardingRuleInventory portForwardingRuleInventory = createPortForwardingRule {
            name = "port"
            vipPortStart = 21
            vipPortEnd = 21
            protocolType = "TCP"
            vipUuid = vipInventory.uuid
        }

        expect([AssertionError.class]){
            deletePortForwardingRule {
                uuid = portForwardingRuleInventory.uuid
                sessionId = testSessionInventory.uuid
            }
        }
    }

    void testSecurityGroup(){
        SecurityGroupInventory securityGroupInventory = createSecurityGroup {
            name = "testSecurityGroup"
            description = "desc"
        }

        expect([AssertionError.class]){
            deleteSecurityGroup {
                uuid = securityGroupInventory.uuid
                sessionId = testSessionInventory.uuid
            }
        }
    }
}
