package org.zstack.test.integration.storage

import org.zstack.compute.zone.ManagementNetworkIpVersionManagerImpl
import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.apimediator.ApiMessageInterceptionException
import org.zstack.header.host.ConnectHostMsg
import org.zstack.header.host.ConnectHostReply
import org.zstack.header.host.CpuArchitecture
import org.zstack.header.storage.backup.BackupStorageState
import org.zstack.header.storage.backup.BackupStorageStatus
import org.zstack.header.storage.backup.BackupStorageVO
import org.zstack.header.storage.backup.BackupStorageZoneRefVO
import org.zstack.header.storage.primary.PrimaryStorageState
import org.zstack.header.storage.primary.PrimaryStorageStatus
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.tag.SystemTagVO
import org.zstack.header.tag.SystemTagVO_
import org.zstack.header.zone.ZoneVO
import org.zstack.kvm.KVMHostVO
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.SystemTagInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.storage.backup.sftp.SftpBackupStorageCommands
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant
import org.zstack.storage.ceph.CephConstants
import org.zstack.storage.ceph.MonStatus
import org.zstack.storage.ceph.backup.CephBackupStorageMonVO
import org.zstack.storage.ceph.backup.CephBackupStorageVO
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonVO
import org.zstack.storage.ceph.primary.CephPrimaryStorageVO
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.network.ManagementNetworkIpVersionUtils

import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_STORAGE_PRIMARY_10055

class ManagementNetworkIpVersionStorageConstraintCase extends SubCase {
    private static final String IPV4 = "192.168.10.10"
    private static final String IPV6 = "2001:db8::10"
    private static final String IPV4_NFS_URL = "${IPV4}:/export/nfs"
    private static final String IPV6_NFS_URL = "[${IPV6}]:/export/nfs"
    private static final String REAL_IPV6_ZONE = "real-ipv6-zone"
    private static final String REAL_IPV6_TAG = "managementNetwork::ipVersion::${ManagementNetworkIpVersionUtils.IPV6}"

    private EnvSpec env
    private DatabaseFacade dbf
    private ManagementNetworkIpVersionManagerImpl manager
    private SystemTagInventory realIpv6Tag
    private final List<List> cleanupItems = []

    @Override
    void clean() {
        if (dbf != null) {
            cleanupItems.reverseEach { item ->
                dbf.removeByPrimaryKey(item[0], item[1] as Class)
            }
        }
        env?.delete()
    }

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            zone {
                name = REAL_IPV6_ZONE
                description = "real ipv6 management network cloud"
            }
        }
    }

    @Override
    void test() {
        env.create {
            dbf = bean(DatabaseFacade.class)
            manager = bean(ManagementNetworkIpVersionManagerImpl.class)
            prepareRealIpv6Cloud()

            testDefaultZoneIpVersion()
            testZoneIpVersionSystemTag()
            testDuplicateZoneIpVersionSystemTagRejected()
            testUpdateZoneIpVersionTagAllowsReplacingCurrentTag()
            testIpv6LinkLocalEndpointRejected()
            testUpdateZoneIpVersionTagChecksExistingResources()
            testRealCloudExistingIpv6ResourcesBlockZoneIpVersionChange()
            testExistingBackupStorageBlocksZoneIpVersionChange()
            testExistingCephMonBlocksZoneIpVersionChange()
        }
    }

    void testDefaultZoneIpVersion() {
        ZoneInventory zone = createTestZone()

        assert manager.getZoneIpVersion(zone.uuid) == ManagementNetworkIpVersionUtils.IPV4
        SystemTagVO tag = getOnlyZoneIpVersionTag(zone.uuid)
        assert tag.tag == "managementNetwork::ipVersion::${ManagementNetworkIpVersionUtils.IPV4}"
        assert !tag.inherent
    }

    void testZoneIpVersionSystemTag() {
        String tag = "managementNetwork::ipVersion::${ManagementNetworkIpVersionUtils.IPV6}"
        ZoneInventory zone = createZone {
            name = "zone-${Platform.uuid}"
            systemTags = [tag]
        } as ZoneInventory
        cleanupItems << [getOnlyZoneIpVersionTag(zone.uuid).uuid, SystemTagVO.class]
        cleanupItems << [zone.uuid, ZoneVO.class]

        assert manager.getZoneIpVersion(zone.uuid) == ManagementNetworkIpVersionUtils.IPV6
        assert getZoneIpVersionTags(zone.uuid).size() == 1
        manager.validateZoneIpVersionTagValue(zone.uuid, tag)
        expect(ApiMessageInterceptionException.class) {
            manager.validateZoneIpVersionTagValue(zone.uuid, "managementNetwork::ipVersion::dual")
        }
    }

    void testDuplicateZoneIpVersionSystemTagRejected() {
        ZoneInventory zone = createTestZone()

        expect(AssertionError.class) {
            createZoneIpVersionTag(zone.uuid, "managementNetwork::ipVersion::${ManagementNetworkIpVersionUtils.IPV6}")
        }
    }

    void testUpdateZoneIpVersionTagAllowsReplacingCurrentTag() {
        ZoneInventory zone = createTestZone()
        SystemTagVO currentTag = getOnlyZoneIpVersionTag(zone.uuid)

        updateSystemTag {
            uuid = currentTag.uuid
            tag = "managementNetwork::ipVersion::${ManagementNetworkIpVersionUtils.IPV6}"
        }

        assert manager.getZoneIpVersion(zone.uuid) == ManagementNetworkIpVersionUtils.IPV6
    }

    void testIpv6LinkLocalEndpointRejected() {
        ZoneInventory zone = createTestZone()
        updateSystemTag {
            uuid = getOnlyZoneIpVersionTag(zone.uuid).uuid
            tag = "managementNetwork::ipVersion::${ManagementNetworkIpVersionUtils.IPV6}"
        }

        expect(ApiMessageInterceptionException.class) {
            manager.validateEndpointInZone(zone.uuid, "[fe80::10]:/data", "primary storage", "ps",
                    ORG_ZSTACK_STORAGE_PRIMARY_10055)
        }
    }

    void testUpdateZoneIpVersionTagChecksExistingResources() {
        ZoneInventory zone = createTestZone()
        createPrimaryStorage(zone.uuid, IPV4_NFS_URL)
        SystemTagVO currentTag = getOnlyZoneIpVersionTag(zone.uuid)

        expect(AssertionError.class) {
            updateSystemTag {
                uuid = currentTag.uuid
                tag = "managementNetwork::ipVersion::${ManagementNetworkIpVersionUtils.IPV6}"
            }
        }
    }

    void testRealCloudExistingIpv6ResourcesBlockZoneIpVersionChange() {
        ZoneInventory zone = env.inventoryByName(REAL_IPV6_ZONE) as ZoneInventory

        expect(ApiMessageInterceptionException.class) {
            manager.validateZoneCompatibleWithExistingResources(zone.uuid, ManagementNetworkIpVersionUtils.IPV4)
        }

        expect(AssertionError.class) {
            updateSystemTag {
                uuid = realIpv6Tag.uuid
                tag = "managementNetwork::ipVersion::${ManagementNetworkIpVersionUtils.IPV4}"
            }
        }

        expect(AssertionError.class) {
            deleteTag {
                uuid = realIpv6Tag.uuid
            }
        }
    }

    private void prepareRealIpv6Cloud() {
        ZoneInventory zone = env.inventoryByName(REAL_IPV6_ZONE) as ZoneInventory
        realIpv6Tag = updateSystemTag {
            uuid = getOnlyZoneIpVersionTag(zone.uuid).uuid
            tag = REAL_IPV6_TAG
        } as SystemTagInventory

        env.message(ConnectHostMsg.class) { ConnectHostMsg msg, CloudBus bus ->
            KVMHostVO host = dbf.findByUuid(msg.uuid, KVMHostVO.class)
            host.setArchitecture(CpuArchitecture.x86_64.name())
            host.setOsDistribution("centos")
            host.setOsRelease("core")
            host.setOsVersion("7.6.1810")
            dbf.update(host)
            bus.reply(msg, new ConnectHostReply())
        }

        ClusterInventory cluster = createCluster {
            name = "real-ipv6-cluster"
            hypervisorType = "KVM"
            zoneUuid = zone.uuid
        } as ClusterInventory

        def primaryStorage = addNfsPrimaryStorage {
            name = "real-ipv6-nfs"
            url = IPV6_NFS_URL
            zoneUuid = zone.uuid
        }

        attachPrimaryStorageToCluster {
            primaryStorageUuid = primaryStorage.uuid
            clusterUuid = cluster.uuid
        }

        addKVMHost {
            name = "real-ipv6-kvm"
            managementIp = IPV6
            username = "root"
            password = "password"
            clusterUuid = cluster.uuid
        }

        env.simulator(SftpBackupStorageConstant.CONNECT_PATH) {
            def rsp = new SftpBackupStorageCommands.ConnectResponse()
            rsp.availableCapacity = SizeUnit.GIGABYTE.toByte(1000)
            rsp.totalCapacity = SizeUnit.GIGABYTE.toByte(1000)
            return rsp
        }

        def backupStorage = addSftpBackupStorage {
            name = "real-ipv6-sftp"
            url = "/sftp"
            username = "root"
            password = "password"
            hostname = IPV6
        }

        attachBackupStorageToZone {
            backupStorageUuid = backupStorage.uuid
            zoneUuid = zone.uuid
        }
    }

    void testExistingBackupStorageBlocksZoneIpVersionChange() {
        ZoneInventory zone = createTestZone()
        createBackupStorage(zone.uuid, IPV6_NFS_URL)

        expect(ApiMessageInterceptionException.class) {
            manager.validateZoneCompatibleWithExistingResources(zone.uuid, ManagementNetworkIpVersionUtils.IPV4)
        }
    }

    void testExistingCephMonBlocksZoneIpVersionChange() {
        ZoneInventory zone = createTestZone()
        createCephPrimaryStorage(zone.uuid, IPV4)
        createCephBackupStorage(zone.uuid, IPV6)

        expect(ApiMessageInterceptionException.class) {
            manager.validateZoneCompatibleWithExistingResources(zone.uuid, ManagementNetworkIpVersionUtils.IPV6)
        }
        expect(ApiMessageInterceptionException.class) {
            manager.validateZoneCompatibleWithExistingResources(zone.uuid, ManagementNetworkIpVersionUtils.IPV4)
        }
    }

    private ZoneInventory createTestZone() {
        ZoneInventory zone = createZone {
            name = "zone-${Platform.uuid}"
        } as ZoneInventory
        cleanupItems << [getOnlyZoneIpVersionTag(zone.uuid).uuid, SystemTagVO.class]
        cleanupItems << [zone.uuid, ZoneVO.class]
        return zone
    }

    private SystemTagVO getOnlyZoneIpVersionTag(String zoneUuid) {
        List<SystemTagVO> tags = getZoneIpVersionTags(zoneUuid)
        assert tags.size() == 1
        return tags[0]
    }

    private List<SystemTagVO> getZoneIpVersionTags(String zoneUuid) {
        return Q.New(SystemTagVO.class)
                .eq(SystemTagVO_.resourceUuid, zoneUuid)
                .eq(SystemTagVO_.resourceType, ZoneVO.simpleName)
                .like(SystemTagVO_.tag, "managementNetwork::ipVersion::%")
                .list()
    }

    private SystemTagInventory createZoneIpVersionTag(String zoneUuid, String tagValue) {
        SystemTagInventory inventory = createSystemTag {
            resourceUuid = zoneUuid
            resourceType = ZoneVO.simpleName
            tag = tagValue
        } as SystemTagInventory
        cleanupItems << [inventory.uuid, SystemTagVO.class]
        return inventory
    }

    private void createPrimaryStorage(String zoneUuid, String url) {
        PrimaryStorageVO ps = new PrimaryStorageVO()
        ps.uuid = Platform.uuid
        ps.name = "ps-${ps.uuid}"
        ps.zoneUuid = zoneUuid
        ps.url = url
        ps.mountPath = "/mnt/${ps.uuid}"
        ps.type = "NFS"
        ps.state = PrimaryStorageState.Enabled
        ps.status = PrimaryStorageStatus.Connected
        dbf.persist(ps)
        cleanupItems << [ps.uuid, PrimaryStorageVO.class]
    }

    private void createBackupStorage(String zoneUuid, String url) {
        BackupStorageVO bs = new BackupStorageVO()
        bs.uuid = Platform.uuid
        bs.name = "bs-${bs.uuid}"
        bs.type = "ImageStoreBackupStorage"
        bs.url = url
        bs.state = BackupStorageState.Enabled
        bs.status = BackupStorageStatus.Connected
        dbf.persist(bs)
        cleanupItems << [bs.uuid, BackupStorageVO.class]
        createBackupStorageZoneRef(bs.uuid, zoneUuid)
    }

    private void createCephPrimaryStorage(String zoneUuid, String monHostname) {
        CephPrimaryStorageVO ps = new CephPrimaryStorageVO()
        ps.uuid = Platform.uuid
        ps.name = "ceph-ps-${ps.uuid}"
        ps.zoneUuid = zoneUuid
        ps.type = CephConstants.CEPH_PRIMARY_STORAGE_TYPE
        ps.url = "not used"
        ps.mountPath = "/mnt/${ps.uuid}"
        ps.state = PrimaryStorageState.Enabled
        ps.status = PrimaryStorageStatus.Connected
        dbf.persist(ps)
        cleanupItems << [ps.uuid, CephPrimaryStorageVO.class]

        CephPrimaryStorageMonVO mon = new CephPrimaryStorageMonVO()
        mon.uuid = Platform.uuid
        mon.primaryStorageUuid = ps.uuid
        mon.hostname = monHostname
        mon.monAddr = monHostname
        mon.sshUsername = "root"
        mon.sshPassword = "password"
        mon.status = MonStatus.Connected
        dbf.persist(mon)
        cleanupItems << [mon.uuid, CephPrimaryStorageMonVO.class]
    }

    private void createCephBackupStorage(String zoneUuid, String monHostname) {
        CephBackupStorageVO bs = new CephBackupStorageVO()
        bs.uuid = Platform.uuid
        bs.name = "ceph-bs-${bs.uuid}"
        bs.type = CephConstants.CEPH_BACKUP_STORAGE_TYPE
        bs.url = "not used"
        bs.poolName = "test-pool"
        bs.state = BackupStorageState.Enabled
        bs.status = BackupStorageStatus.Connected
        dbf.persist(bs)
        cleanupItems << [bs.uuid, CephBackupStorageVO.class]
        createBackupStorageZoneRef(bs.uuid, zoneUuid)

        CephBackupStorageMonVO mon = new CephBackupStorageMonVO()
        mon.uuid = Platform.uuid
        mon.backupStorageUuid = bs.uuid
        mon.hostname = monHostname
        mon.monAddr = monHostname
        mon.sshUsername = "root"
        mon.sshPassword = "password"
        mon.status = MonStatus.Connected
        dbf.persist(mon)
        cleanupItems << [mon.uuid, CephBackupStorageMonVO.class]
    }

    private void createBackupStorageZoneRef(String backupStorageUuid, String zoneUuid) {
        BackupStorageZoneRefVO ref = new BackupStorageZoneRefVO()
        ref.backupStorageUuid = backupStorageUuid
        ref.zoneUuid = zoneUuid
        dbf.persist(ref)
        cleanupItems << [ref.id, BackupStorageZoneRefVO.class]
    }
}
