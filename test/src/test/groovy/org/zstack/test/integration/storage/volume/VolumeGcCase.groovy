package org.zstack.test.integration.storage.volume

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.core.gc.GCStatus
import org.zstack.core.gc.GarbageCollectorVO
import org.zstack.core.gc.GarbageCollectorVO_
import org.zstack.header.volume.VolumeDeletionPolicyManager
import org.zstack.sdk.DiskOfferingInventory
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

import java.util.concurrent.TimeUnit

class VolumeGcCase extends SubCase {
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
        env = CephEnv.CephStorageOneVmEnv()
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
            testDeleteVolumeGcExtension()
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

    void testDeleteVolumeGcExtension() {
        VolumeInventory vol1 = createDataVolume {
            name = "data1"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ceph.uuid
        }

        VolumeInventory vol2 = createDataVolume {
            name = "data2"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ceph.uuid
        }

        VolumeInventory vol3 = createDataVolume {
            name = "data3"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ceph.uuid
        }

        deleteFail = true

        deleteDataVolume {
            uuid = vol1.uuid
        }

        deleteDataVolume {
            uuid = vol2.uuid
        }

        deleteDataVolume {
            uuid = vol3.uuid
        }

        List<GarbageCollectorVO> cephVo = Q.New(GarbageCollectorVO.class).list()

        cephVo.stream().forEach({ item ->
            for (int i = 100; i < 200; i++) {
                item.uuid = String.format(getContextVolumeUuid(item).substring(0,29) + i)
                dbf.persist(item)
            }
        });

        long count = Q.New(GarbageCollectorVO.class)
                .eq(GarbageCollectorVO_.runnerClass, CephDeleteVolumeGC.getName())
                .eq(GarbageCollectorVO_.status, GCStatus.Idle)
                .count()

        SQL.New("select vo from GarbageCollectorVO vo where vo.runnerClass = :runnerClass and vo.status = :status")
                .param("runnerClass", CephDeleteVolumeGC.getName())
                .param("status", GCStatus.Idle)
                .limit(10).paginate(count, { List<GarbageCollectorVO> vos ->
            vos.forEach({ vo ->
                SQL.New("delete from GarbageCollectorVO vo " +
                        "where vo.runnerClass = :runnerClass " +
                        "and vo.status = :status " +
                        "and vo.uuid in (select vo.uuid from GarbageCollectorVO vo where vo.uuid not in " +
                        "(select min(vo.uuid) from GarbageCollectorVO vo group by substring(cast(vo.context as string), '19', '34')))")
                        .param("runnerClass", CephDeleteVolumeGC.getName())
                        .param("status", GCStatus.Idle)
                        .execute()
            })
        });

        assert Q.New(GarbageCollectorVO.class)
                .eq(GarbageCollectorVO_.runnerClass, CephDeleteVolumeGC.getName())
                .eq(GarbageCollectorVO_.status, GCStatus.Idle)
                .count() == 3
    }

    String getContextVolumeUuid(GarbageCollectorVO vo) {
        String context = vo.getContext()
        JsonParser jp = new JsonParser();
        JsonObject jo = jp.parse(context).getAsJsonObject();
        return jo.get("volume").getAsJsonObject().get("uuid").getAsString()
    }
}
