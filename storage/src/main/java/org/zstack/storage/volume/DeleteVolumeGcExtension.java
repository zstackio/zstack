package org.zstack.storage.volume;

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
import org.zstack.utils.BeanUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DeleteVolumeGcExtension implements Component {
    protected static final CLogger logger = Utils.getLogger(DeleteVolumeGcExtension.class);

    @Autowired
    private DatabaseFacade dbf;

    @Autowired
    private ThreadFacade thdf;

    private DeleteVolumeOnPrimaryStorageGC deleteVolumeOnPrimaryStorageGC;

    @Override
    public boolean start() {
        BeanUtils.reflections.getSubTypesOf(DeleteVolumeOnPrimaryStorageGC.class).forEach(clz -> {
            DeleteVolumeOnPrimaryStorageGC gc;
            try {
                gc = clz.getConstructor().newInstance();
                DeleteVolumeGC(gc);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
//            if (DELETE_VOLUME_GC) {
//                thdf.submitTimerTask(this::DeleteVolumeGC, TimeUnit.MINUTES, 5);
//            }
        });
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

    boolean DeleteVolumeGC(DeleteVolumeOnPrimaryStorageGC deleteVolumeOnPrimaryStorageGC) {
        long count = Q.New(GarbageCollectorVO.class)
                .eq(GarbageCollectorVO_.runnerClass, deleteVolumeOnPrimaryStorageGC)
                .eq(GarbageCollectorVO_.status, GCStatus.Idle)
                .count();
        Map<String, GarbageCollectorVO> mapVo = new HashMap<>();
        SQL.New("select vo from GarbageCollectorVO vo where vo.runnerClass = :runnerClass and vo.status = :status")
                .param("runnerClass", deleteVolumeOnPrimaryStorageGC)
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
