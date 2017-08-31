package org.zstack.test.integration.storage.ceph

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.tag.SystemTagVO
import org.zstack.header.tag.SystemTagVO_
import org.zstack.sdk.AddCephPrimaryStorageAction
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonBase
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test
import org.zstack.utils.data.SizeUnit

/**
 * Created by AlanJager on 2017/9/1.
 */
class AddCephWithAliasNameCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = Test.makeEnv {
            zone {
                name = "zone1"
                cluster {
                    name = "cluster1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "host1"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                        usedMem = 1000
                        totalCpu = 10
                    }

                    attachL2Network("l2")
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"



                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }
                }

            }
        }
    }

    void testAddCephWithAliasPoolName() {
        String imageCachePoolName = "ceph::alias::imageCachePoolName::imageCachePool"
        String rootVolumePoolName = "ceph::alias::rootVolumePoolName::rootVolumePool"
        String dataVolumePoolName = "ceph::alias::dataVolumePoolName::dataVolumePool"
        String fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"

        env.simulator(CephPrimaryStorageMonBase.ECHO_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            return [:]
        }

        env.simulator(CephPrimaryStorageBase.GET_FACTS) { HttpEntity<String> e, EnvSpec spec ->
            def rsp = new CephPrimaryStorageBase.GetFactsRsp()
            return rsp
        }

        env.simulator(CephPrimaryStorageBase.INIT_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def rsp = new CephPrimaryStorageBase.InitRsp()
            rsp.fsid = fsid
            rsp.availableCapacity = SizeUnit.GIGABYTE.toByte(1)
            rsp.totalCapacity = SizeUnit.GIGABYTE.toByte(1)
            return rsp
        }

        AddCephPrimaryStorageAction action = new AddCephPrimaryStorageAction()
        action.name = "ceph-primary-new"
        action.monUrls = ["root:password@localhost"]
        action.rootVolumePoolName = "pool-xxxx"
        action.dataVolumePoolName = "pool-xxxx"
        action.imageCachePoolName = "pool-xxxx"
        action.zoneUuid = env.inventoryByName("zone1").uuid
        action.systemTags = [imageCachePoolName, rootVolumePoolName, dataVolumePoolName]
        action.sessionId = adminSession()
        AddCephPrimaryStorageAction.Result ret = action.call()

        assert ret.error == null
        String tag = Q.New(SystemTagVO.class)
                .select(SystemTagVO_.tag)
                .eq(SystemTagVO_.resourceUuid, ret.value.inventory.getUuid())
                .like(SystemTagVO_.tag, "%alias::imageCache%").findValue()
        assert tag == imageCachePoolName

        tag = Q.New(SystemTagVO.class)
                .select(SystemTagVO_.tag)
                .eq(SystemTagVO_.resourceUuid, ret.value.inventory.getUuid())
                .like(SystemTagVO_.tag, "%alias::rootVolumePoolName%").findValue()
        assert tag == rootVolumePoolName

        tag = Q.New(SystemTagVO.class)
                .select(SystemTagVO_.tag)
                .eq(SystemTagVO_.resourceUuid, ret.value.inventory.getUuid())
                .like(SystemTagVO_.tag, "%alias::dataVolumePoolName%").findValue()
        assert tag == dataVolumePoolName
    }

    @Override
    void test() {
        env.create {
            testAddCephWithAliasPoolName()
        }
    }
}
