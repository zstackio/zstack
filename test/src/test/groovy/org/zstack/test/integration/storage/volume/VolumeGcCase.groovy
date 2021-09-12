package org.zstack.test.integration.storage.volume

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.zstack.core.db.DatabaseFacade
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

        GarbageCollectorVO cephVo = Q.New(GarbageCollectorVO.class).limit(1)find()

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

        assert Q.New(GarbageCollectorVO.class)
                .eq(GarbageCollectorVO_.runnerClass, CephDeleteVolumeGC.getName())
                .eq(GarbageCollectorVO_.status, GCStatus.Idle)
                .count() == 1
    }

    String getContextVolumeUuid(GarbageCollectorVO vo) {
        String context = vo.getContext()
        JsonParser jp = new JsonParser();
        JsonObject jo = jp.parse(context).getAsJsonObject();
        return jo.get("volume").getAsJsonObject().get("uuid").getAsString()
    }

}
