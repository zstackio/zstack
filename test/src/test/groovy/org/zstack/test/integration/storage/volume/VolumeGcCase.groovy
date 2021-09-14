package org.zstack.test.integration.storage.volume

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.core.db.SQLBatch
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
        List<GarbageCollectorVO> vos = new ArrayList()
        cephVo.each { it ->
            for (int i = 100000; i < 100999; i++) {
                GarbageCollectorVO vo = new GarbageCollectorVO()
                vo.uuid = String.format(getContextVolumeUuid(it).substring(0, 26) + i)
                vo.status = it.status
                vo.context =it.context
                vo.name =it.name
                vo.managementNodeUuid =it.managementNodeUuid
                vo.runnerClass =it.runnerClass
                vo.lastOpDate = it.lastOpDate
                vo.createDate = it.createDate
                vo.type = it.type
                vo.resourceName = it.resourceName
                vo.resourceType = it.resourceType
                vo.concreteResourceType = it.concreteResourceType
                vos.add(vo)
            }
        }
        dbf.persistCollection(vos)

        def now1 = new Date()

        long count = Q.New(GarbageCollectorVO.class)
                .eq(GarbageCollectorVO_.runnerClass, CephDeleteVolumeGC.getName())
                .eq(GarbageCollectorVO_.status, GCStatus.Idle)
                .count()
        Map<String, GarbageCollectorVO> mapVo = [:]
        SQL.New("select vo from GarbageCollectorVO vo where vo.runnerClass = :runnerClass and vo.status = :status")
                .param("runnerClass", CephDeleteVolumeGC.getName())
                .param("status", GCStatus.Idle)
                .limit(1000).paginate(count, { List<GarbageCollectorVO> gcvos -> gcvos.forEach({ vo ->
            mapVo.put(getContextVolumeUuid(vo), vo)
        })})
        def now2 = new Date()

        SQL.New("delete from GarbageCollectorVO gc").execute();
        List<GarbageCollectorVO> res = new ArrayList(mapVo.values());
        dbf.persistCollection(res)
        def now3 = new Date()

        assert Q.New(GarbageCollectorVO.class)
                .eq(GarbageCollectorVO_.runnerClass, CephDeleteVolumeGC.getName())
                .eq(GarbageCollectorVO_.status, GCStatus.Idle)
                .count() == 3

//        select count(*) from GarbageCollectorVO where runnerClass="org.zstack.storage.ceph.primary.CephDeleteVolumeGC" and status="Idle";
//
//        select * from GarbageCollectorVO where runnerClass="org.zstack.storage.ceph.primary.CephDeleteVolumeGC";
//
//        select * from GarbageCollectorVO where runnerClass="org.zstack.storage.ceph.primary.CephDeleteVolumeGC" group by (substring_index(context, ':', '-1'));
//
//        delete from GarbageCollectorVO;

//        Map<String, GarbageCollectorVO> mapVo = [:]
//        SQL.New("select vo, min(vo.uuid) from GarbageCollectorVO vo where vo.runnerClass = :runnerClass and vo.status = :status " +
//                "group by substring(cast(vo.context as string), '19', '34')")
//                .param("runnerClass", CephDeleteVolumeGC.getName())
//                .param("status", GCStatus.Idle)
//                .limit(1000).paginate(count, { List<GarbageCollectorVO> gcvos -> gcvos.forEach({ vo ->
//            mapVo.put(getContextVolumeUuid(vo), vo)
//        })})
//        mapVo
//        SQL.New("delete from GarbageCollectorVO gc").execute();
//
//        new SQLBatch() {
//            @Override
//            protected void scripts() {
//                int times = (int) (300000 / 1000) + (300000 % 1000 != 0 ? 1 : 0);
//                for (int i=0;i<times;i++){
//                    sql("delete from GarbageCollectorVO limit 1000").execute();
//                }
//            }
//        }
    }

    String getContextVolumeUuid(GarbageCollectorVO vo) {
        String context = vo.getContext()
        JsonParser jp = new JsonParser();
        JsonObject jo = jp.parse(context).getAsJsonObject();
        return jo.get("volume").getAsJsonObject().get("uuid").getAsString()
    }
}
