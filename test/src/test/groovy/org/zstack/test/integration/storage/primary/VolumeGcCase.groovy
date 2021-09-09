package org.zstack.test.integration.storage.primary

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
import org.zstack.utils.gson.JSONObjectUtil

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
            testVolumeGCSuccess()
            deleteVolumeGcExtension()
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

    void testVolumeGCSuccess() {
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

        assert deleteVolumeGcExtension() != 0
    }

    long deleteVolumeGcExtension() {
        long count = Q.New(GarbageCollectorVO.class)
                .eq(GarbageCollectorVO_.runnerClass, CephDeleteVolumeGC.getName())
                .eq(GarbageCollectorVO_.status, GCStatus.Idle)
                .count()

        Map<String, GarbageCollectorVO> mapVo = new HashMap<>();
        SQL.New("select vo from GarbageCollectorVO vo " +
                "where vo.runnerClass = :runnerClass")
                .param("runnerClass", CephDeleteVolumeGC.getName())
                .limit(1000).paginate(count, { List<GarbageCollectorVO> vids ->
            vids.forEach({ vid ->
                mapVo.put(getContextVolumeUuid(vid), vid)
                    SQL.New(vid.class).delete()
            })
        });
        List<String> result2 = new ArrayList(mapVo.values());
        for (int i = 0; i < result2.size(); i++) {
            dbf.persist(result2[i])
        }
        return count
    }

    String getContextVolumeUuid(GarbageCollectorVO vo){
        String context = vo.getContext()
        JsonParser jp = new JsonParser();
        JsonObject jo = jp.parse(context).getAsJsonObject();
        String VolumeUuid = jo.get("volume").get("uuid").getAsString()
        return VolumeUuid
    }
}
//            Long tuples1 = SQL.New("select count(vo.uuid) from GarbageCollectorVO vo group by substring(cast(vo.context as string), '19', '34')").find()
//            String tuples2 = SQL.New("select substring(cast(vo.context as string), '19', '34') from GarbageCollectorVO vo").find()
//
//            if ( tuples1 != 1 ) {
//                mapvo.put(tuples2, vid)
//                SQL.New(vid.class).delete()
//            }
//            SQL.New("select substring(cast(vo.context as string), '19', '34')").find()
//            SQL.New("select substring(cast(vo.context as string), '19', '34')")
//            SQL.New("select dt.min from (select min(vo.id) as min from GarbageCollectorVO vo group by substring(cast(vo.context as string), '19', '34'))").find()
//
//            SQL.New("delete from GarbageCollectorVO vo where vo.runnerClass = :=runnerClass and vo.uuid not in ( select dt.minno from ( select min(vo.uuid) as minno from GarbageCollectorVO group by substring(cast(vo.context as string), '19', '34')) dt")
//                    .param("runnerClass", CephDeleteVolumeGC.getName())
//                    .execute()
//            SQL.New("delete from GarbageCollectorVO vo where vo.uuid not in ( select dt.minno from ( select min(vo.uuid) as minno from GarbageCollectorVO group by substring(cast(vo.context as string), '19', '34')) dt").execute()
//            SQL.New("select min(vo.uuid) from GarbageCollectorVO vo group by substring(cast(vo.context as string), '19', '34')").find()
//            SQL.New("select vo.uuid from (select min(vo.uuid) from GarbageCollectorVO vo group by substring(cast(vo.context as string), '19', '34')))").find()
