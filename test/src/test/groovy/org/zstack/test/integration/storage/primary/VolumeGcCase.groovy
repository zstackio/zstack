package org.zstack.test.integration.storage.primary

import groovy.sql.Sql
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

import java.util.concurrent.TimeUnit

class VolumeGcCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf
    Q q
    SQL sql
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
            q  = bean(Q.class)
            sql = bean(SQL.class)
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

        GarbageCollectorVO cephVo = Q.New(GarbageCollectorVO_.class).eq(CephDeleteVolumeGC.class).find()

        for (int i = 0; i < 100; i++) {
            dbf.persist(cephVo);
        }

        assert deleteVolumeGcExtension() != 0

        GarbageCollectorInventory inv = null

        retryInSecs {
            inv = queryGCJob {
                conditions = ["context~=%${vol.getUuid()}%".toString()]
            }[0]

            assert inv.status == GCStatus.Idle.toString()
            assert called == true
        }

        assert queryGCJob {
            conditions = ["context~=%${vol.getUuid()}%".toString()]
        }.size == 1

        called = false
        deleteFail = false

        triggerGCJob {
            uuid = inv.uuid
        }

        retryInSecs {
            inv = queryGCJob {
                conditions = ["context~=%${vol.getUuid()}%".toString()]
            }[0]

            assert called
            assert inv.status == GCStatus.Done.toString()
        }
    }

    long deleteVolumeGcExtension() {
        long count = SQL.New("select GarbageCollectorVO.context from GarbageCollectorVO vo " +
                "where vo.runnerClass = :runnerClass and vo.status := status", Long.class)
                .param("runnerClass", CephDeleteVolumeGC.class)
                .param("status", GCStatus.Idle)
                .find();
        logger.debug(String.format("%s", count));
        return count
        //        SQL.New("select GarbageCollectorVO.context from GarbageCollectorVO vo " +
//                "where vo.runnerClass = :runnerClass and vo.status := status", String.class)
//                .param("runnerClass", DeleteVolumeGC.class)
//                .param("status", GCStatus.Idle.toString())
//                .limit(500).paginate(count, (List<String> vids) -> vids.forEach(vid -> {
//
//            if (!Q.New(GarbageCollectorVO.class)
//                    .eq(GarbageCollectorVO_.runnerClass, vid)
//                    .isExists()) {
//                IAM2VirtualIDOrganizationRefVO refVO = new IAM2VirtualIDOrganizationRefVO();
//                refVO.setVirtualIDUuid(vid);
//                refVO.setOrganizationUuid(IAM2Constant.INITIAL_ORGANIZATION_DEFAULT_UUID);
//                dbf.persist(refVO);
//            }
//
//            SQL.New(GarbageCollectorVO.class).delete();
//        }));
    }
}
