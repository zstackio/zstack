import com.google.gson.JsonObject
import com.google.gson.JsonParser
import groovy.sql.Sql
import org.apache.logging.log4j.core.layout.JacksonFactory
import org.json.JSONObject
import org.zstack.appliancevm.ApplianceVmVO
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.core.gc.GCStatus
import org.zstack.core.gc.GarbageCollectorVO
import org.zstack.core.gc.GarbageCollectorVO_
import org.zstack.header.image.ImageConstant
import org.zstack.header.volume.VolumeDeletionPolicyManager
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.GarbageCollectorInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.storage.ceph.CephGlobalConfig
import org.zstack.storage.ceph.primary.CephDeleteVolumeGC
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.storage.volume.VolumeGlobalConfig
import org.zstack.test.integration.storage.CephEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.DiskOfferingSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HttpError
import org.zstack.testlib.PrimaryStorageSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import java.util.concurrent.TimeUnit

class CephVolumeGcCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf
    PrimaryStorageInventory ceph
    DiskOfferingInventory diskOffering
    boolean deleteFail = false
    boolean called = false

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
        //env = CephEnv.CephStorageOneVmEnv()
        env = makeEnv {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }
            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(20)
            }
            zone {
                name = "zone"
                cluster {
                    name = "test-cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "ceph-mon"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                        usedMem = 1000
                        totalCpu = 10
                    }
                    kvm {
                        name = "host"
                        username = "root"
                        password = "password"
                        usedMem = 1000
                        totalCpu = 10
                    }

                    attachPrimaryStorage("ceph-pri")
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

                cephPrimaryStorage {
                    name = "ceph-pri"
                    description = "Test"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    availableCapacity = SizeUnit.GIGABYTE.toByte(100)
                    url = "ceph://pri"
                    fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls = ["root:password@localhost/?monPort=7777"]
                }


                attachBackupStorage("ceph-bk")
            }

            cephBackupStorage {
                name = "ceph-bk"
                description = "Test"
                totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                availableCapacity = SizeUnit.GIGABYTE.toByte(100)
                url = "/bk"
                fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"
                monUrls = ["root:password@localhost/?monPort=7777"]

                image {
                    name = "test-iso"
                    mediaType = ImageConstant.ImageMediaType.ISO.toString()
                    url = "http://zstack.org/download/test.iso"
                }
                image {
                    name = "image"
                    url = "http://zstack.org/download/image.qcow2"
                }
            }

            vm {
                name = "test-vm"
                useCluster("test-cluster")
                useHost("host")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
                useRootDiskOffering("diskOffering")
                useImage("image")

            }
        }
    }


    @Override
    void test() {
        env.create {
            dbf = bean(DatabaseFacade.class)

            ceph = (env.specByName("ceph-pri") as PrimaryStorageSpec).inventory
            diskOffering = (env.specByName("diskOffering") as DiskOfferingSpec).inventory

            // set a very long time so the GC won't run, we use API to trigger it
            CephGlobalConfig.GC_INTERVAL.updateValue(TimeUnit.DAYS.toSeconds(1))
            VolumeGlobalConfig.VOLUME_DELETION_POLICY.updateValue(VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct.toString())

            prepareEnv()
            testdeleteVolumeGcExtension()
        }
    }

    void prepareEnv() {
        env.afterSimulator(CephPrimaryStorageBase.DELETE_PATH) { rsp ->
            called = true

            if (deleteFail) {
                throw new HttpError(403, "on purpose")
            }

            return rsp
        }
    }

    void testdeleteVolumeGcExtension() {
        VolumeInventory vol = createDataVolume {
            name = "data"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ceph.uuid
        }

        deleteFail = true

        deleteDataVolume {
            uuid = vol.uuid
        }

        GarbageCollectorVO cephVo = Q.New(GarbageCollectorVO.class).find()

        for (int i = 1000; i < 1999; i++) {
            cephVo.uuid = String.format("11386f1f5d854f4eae27b26b9f" + i)
            dbf.persist(cephVo)
        }

        long count = Q.New(GarbageCollectorVO.class)
                .eq(GarbageCollectorVO_.runnerClass, CephDeleteVolumeGC.getName())
                .eq(GarbageCollectorVO_.status, GCStatus.Idle)
                .count()

        Map<String, GarbageCollectorVO> mapVo = new HashMap<>();
        SQL.New("select vo from GarbageCollectorVO vo where vo.runnerClass = :runnerClass and vo.status = :status")
                .param("runnerClass", CephDeleteVolumeGC.getName())
                .param("status", GCStatus.Idle)
                .limit(1000).paginate(count, { List<GarbageCollectorVO> vos ->
            vos.forEach({ vo ->
                mapVo.put(getContextVolumeUuid(vo), vo)
                SQL.New("delete from GarbageCollectorVO vo where vo.uuid = :uuid").param("uuid", vo.getUuid()).execute();
            })
        });
        List<GarbageCollectorVO> res = new ArrayList(mapVo.values());
        for (int i = 0; i < res.size(); i++) {
            dbf.persist(res[i])
        }

        long count2 = Q.New(GarbageCollectorVO.class)
                .eq(GarbageCollectorVO_.runnerClass, CephDeleteVolumeGC.getName())
                .eq(GarbageCollectorVO_.status, GCStatus.Idle)
                .count()

        assert count2 == 2
    }

    String getContextVolumeUuid(GarbageCollectorVO vo) {
        String context = vo.getContext()
        JsonParser jp = new JsonParser();
        JsonObject jo = jp.parse(context).getAsJsonObject();
        return jo.get("volume").getAsJsonObject().get("uuid").getAsString()
    }
}
