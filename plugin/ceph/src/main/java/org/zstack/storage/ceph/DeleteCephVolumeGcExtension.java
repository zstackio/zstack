package org.zstack.storage.ceph;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.gc.GCStatus;
import org.zstack.core.gc.GarbageCollectorVO;
import org.zstack.core.gc.GarbageCollectorVO_;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.storage.ceph.primary.CephDeleteVolumeGC;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.storage.ceph.DeleteCephVolumeGcGlobalProperty.DELETE_CEPH_VOLUME_GC;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DeleteCephVolumeGcExtension implements Component {
    protected static final CLogger logger = Utils.getLogger(DeleteCephVolumeGcExtension.class);

    @Autowired
    private DatabaseFacade dbf;

    @Autowired
    private ThreadFacade thdf;

    @Override
    public boolean start() {
        if (DELETE_CEPH_VOLUME_GC) {
            thdf.submitTimerTask(this::cephDeleteVolumeGC, TimeUnit.MINUTES, 5);
        }
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private String getContextVolumeUuid(GarbageCollectorVO vo) {
        String context = vo.getContext();
        JsonParser jp = new JsonParser();
        JsonObject jo = jp.parse(context).getAsJsonObject();
        return jo.get("volume").getAsJsonObject().get("uuid").getAsString();
    }

    boolean cephDeleteVolumeGC() {
        long count = Q.New(GarbageCollectorVO.class)
                .eq(GarbageCollectorVO_.runnerClass, CephDeleteVolumeGC.class.getName())
                .eq(GarbageCollectorVO_.status, GCStatus.Idle)
                .count();
        Map<String, GarbageCollectorVO> mapVo = new HashMap<>();
        SQL.New("select vo from GarbageCollectorVO vo where vo.runnerClass = :runnerClass and vo.status = :status")
                .param("runnerClass", CephDeleteVolumeGC.class.getName())
                .param("status", GCStatus.Idle)
                .limit(1000).paginate(count, (List<GarbageCollectorVO> vos) -> vos.forEach(vo -> {
                    mapVo.put(getContextVolumeUuid(vo), vo);
                    SQL.New("delete from GarbageCollectorVO vo where vo.uuid = :uuid").param("uuid", vo.getUuid()).execute();
                }));
        List<GarbageCollectorVO> res = new ArrayList(mapVo.values());
        for (int i = 0; i < res.size(); i++) {
            dbf.persist(res.get(i));
        }
        return true;
    }
}
