package org.zstack.test.integration.network.l3network.ipv6

import org.zstack.header.image.ImageConstant
import org.zstack.sdk.*
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.network.NetworkTest
import org.zstack.test.integration.network.l3network.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.stream.Collectors

/**
 * Created by shixin on 2018/10/16.
 */
class GetCandidateZonesClustersHostsForCreatingVmCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
        useSpring(KvmTest.springSpec)

    }
    @Override
    void environment() {
        env = Env.Ipv6FlatL3Network()
    }

    @Override
    void test() {
        env.create {
            testGetCandidateZonesClustersHostsForCreatingVm()
        }
    }

    void testGetCandidateZonesClustersHostsForCreatingVm() {
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")
        ImageInventory image = env.inventoryByName("image1")
        HostInventory h1 = env.inventoryByName("kvm-1")
        HostInventory h2 = env.inventoryByName("kvm-2")
        BackupStorageInventory bs = env.inventoryByName("sftp")
        BackupStorageInventory bs2 = env.inventoryByName("sftp2")
        ZoneInventory zone = env.inventoryByName("zone")

        ImageInventory iso = addImage {
            name = "sized-image"
            url = "http://my-site/foo.iso"
            backupStorageUuids = [bs.uuid, bs2.uuid]
            format = ImageConstant.ISO_FORMAT_STRING
        }

        expect(AssertionError.class) {
            getCandidateZonesClustersHostsForCreatingVm {
                instanceOfferingUuid = offering.uuid
                imageUuid = iso.uuid
                l3NetworkUuids = [l3_statefull.uuid]
            }
        }

        expect(AssertionError.class) {
            getCandidateZonesClustersHostsForCreatingVm {
                rootDiskOfferingUuid = diskOffering.uuid
                instanceOfferingUuid = offering.uuid
                imageUuid = iso.uuid
                l3NetworkUuids = [l3_statefull.uuid, l3.uuid]
            }
        }

        GetCandidateZonesClustersHostsForCreatingVmResult res = getCandidateZonesClustersHostsForCreatingVm {
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3_statefull.uuid]
        }
        assert res.hosts.stream().map{h -> h.getUuid()}.collect(Collectors.toList()).contains(h1.uuid)
        assert res.hosts.stream().map{h -> h.getUuid()}.collect(Collectors.toList()).contains(h2.uuid)
        assert res.zones.size() == 1
        assert res.clusters.size() == 1

        res = getCandidateZonesClustersHostsForCreatingVm {
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3_statefull.uuid, l3.uuid]
        }
        assert res.hosts.stream().map{h -> h.getUuid()}.collect(Collectors.toList()).contains(h1.uuid)
        assert res.hosts.stream().map{h -> h.getUuid()}.collect(Collectors.toList()).contains(h2.uuid)
        assert res.zones.size() == 1
        assert res.clusters.size() == 1

        res = getCandidateZonesClustersHostsForCreatingVm {
            zoneUuid = zone.uuid
            rootDiskOfferingUuid = diskOffering.uuid
            instanceOfferingUuid = offering.uuid
            imageUuid = iso.uuid
            l3NetworkUuids = [l3_statefull.uuid, l3.uuid]
        }
        assert res.hosts.stream().map{h -> h.getUuid()}.collect(Collectors.toList()).contains(h1.uuid)
        assert res.hosts.stream().map{h -> h.getUuid()}.collect(Collectors.toList()).contains(h2.uuid)
        assert res.zones.size() == 1
        assert res.clusters.size() == 1

        deleteImage {
            uuid = iso.uuid
        }
    }


}

