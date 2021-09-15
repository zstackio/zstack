package org.zstack.storage.volume;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.gc.GCStatus;
import org.zstack.core.gc.GarbageCollectorVO;
import org.zstack.core.gc.GarbageCollectorVO_;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;


@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DeleteVolumeGcExtension implements Component {
    protected static final CLogger logger = Utils.getLogger(DeleteVolumeGcExtension.class);

    @Autowired
    private DatabaseFacade dbf;

    @Autowired
    private ThreadFacade thdf;

    @Override
    public boolean start() {
        deleteVolumeGC();
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

    @Transactional
    public boolean deleteVolumeGC() {
        long count = Q.New(GarbageCollectorVO.class)
                .eq(GarbageCollectorVO_.status, GCStatus.Idle)
                .count();

        HashSet<String> volumeUuids = new HashSet<>();
        SQL.New("select vo from GarbageCollectorVO vo where vo.status = :status and vo.name like ('gc-ceph%' or 'gc-shared-block%' or 'gc-nfs%' or 'gc-smp%' or 'gc-delete-volume%' or 'gc-mini-storage%' or 'gc-aliyun-ebs%' or 'gc-aliyun-nas%')")
                .param("status", GCStatus.Idle)
                .limit(1000).paginate(count, (List<GarbageCollectorVO> vos) -> vos.forEach(vo -> {
                    String volUuid = getContextVolumeUuid(vo);
                    if (volumeUuids.contains(volUuid)) {
                        dbf.getEntityManager().remove(vo);
                    } else {
                        volumeUuids.add(volUuid);
                    }
                }));
        return true;
    }
}
