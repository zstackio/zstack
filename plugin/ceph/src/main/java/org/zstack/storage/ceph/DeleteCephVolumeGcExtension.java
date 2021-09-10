package org.zstack.storage.ceph;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.gc.GCStatus;
import org.zstack.core.gc.GarbageCollectorVO;
import org.zstack.core.gc.GarbageCollectorVO_;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.SessionInventory;
import org.zstack.identity.Session;
import org.zstack.storage.ceph.primary.CephDeleteVolumeGC;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DeleteCephVolumeGcExtension implements Component {
    protected static final CLogger logger = Utils.getLogger(DeleteCephVolumeGcExtension.class);

    @Autowired
    private CloudBus bus;

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;

    private SessionInventory session;

    private long nowTime = System.currentTimeMillis();

    @Override
    public boolean start() {

        GarbageCollectorVO cephVo = new GarbageCollectorVO();
        cephVo.setRunnerClass(CephDeleteVolumeGC.class.getName());
        cephVo.setContext("{\"volume\":{\"uuid\":\"4955018b9768463aa1802fb43340133a\",\"name\":\"data\",\"primaryStorageUuid\":\"2405d68d685445688e2248a0beaad404\",\"diskOfferingUuid\":\"adaf3875d3ce4e1e8b8d602c41887ebf\",\"installPath\":\"ceph://pri-v-d-96743bc999e44ebe86c966cd6c834f69/4955018b9768463aa1802fb43340133a\",\"type\":\"Data\",\"format\":\"raw\",\"size\":21474836480,\"actualSize\":0,\"state\":\"Enabled\",\"status\":\"Ready\",\"createDate\":\"Sep 9, 2021 9:59:38 PM\",\"lastOpDate\":\"Sep 9, 2021 9:59:38 PM\",\"isShareable\":false},\"NEXT_TIME\":86400,\"NEXT_TIME_UNIT\":\"SECONDS\",\"primaryStorageUuid\":\"2405d68d685445688e2248a0beaad404\"}");
        cephVo.setStatus(GCStatus.Idle);
        cephVo.setName("gc-ceph-2405d68d685445688e2248a0beaad404-volume-org.zstack.header.volume.VolumeInventory@71b7ad85");
        cephVo.setType("TimeBased");
        cephVo.setUuid("edface9b8f924c95b498a3d23ed87e4c");

        dbf.persist(cephVo);

        for (int i = 1000; i < 1999; i++) {
            GarbageCollectorVO cephVo1 = cephVo;
            cephVo1.setUuid(String.format("11386f1f5d854f4eae27b26b9f" + i));
            cephVo1.setName(String.format("gc-ceph-2405d68d685445688e2248a0beaad404-volume-org.zstack.header.volume.VolumeInventory@71b7" + i));
            dbf.persist(cephVo1);
        }

//        if (DELETE_CEPH_VOLUME_GC) {
//            thdf.submitTimerTask(this::upgrade, TimeUnit.MINUTES, 5);
//        }
        if (!upgrade()){
            return false;
        }

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private boolean upgrade() {
        return cephDeleteVolumeGC();
    }

    private String getContextVolumeUuid(GarbageCollectorVO vo) {
        String context = vo.getContext();
        JsonParser jp = new JsonParser();
        JsonObject jo = jp.parse(context).getAsJsonObject();
        return jo.get("volume").getAsJsonObject().get("uuid").getAsString();
    }

    boolean cephDeleteVolumeGC() {
        session = Session.login(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
        long count = Q.New(GarbageCollectorVO.class)
                .eq(GarbageCollectorVO_.runnerClass, CephDeleteVolumeGC.class.getName())
                .eq(GarbageCollectorVO_.status, GCStatus.Idle)
                .count();

        Map<String, GarbageCollectorVO> mapVo = new HashMap<>();
        SQL.New("select vo from GarbageCollectorVO vo where vo.runnerClass = :runnerClass and vo.status = :status")
                .param("runnerClass", CephDeleteVolumeGC.class.getName())
                .param("status", GCStatus.Idle)
                .limit(1000).paginate(count, (List<GarbageCollectorVO> vids) -> vids.forEach(vid -> {
                    mapVo.put(getContextVolumeUuid(vid), vid);
                    SQL.New(GarbageCollectorVO.class).delete();
                }));
        List<String> res = new ArrayList(mapVo.values());
        for (int i = 0; i < res.size(); i++) {
            dbf.persist(res.get(i));
        }
        return true;
    }
}
