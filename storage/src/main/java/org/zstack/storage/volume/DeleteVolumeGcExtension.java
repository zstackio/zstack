package org.zstack.storage.volume;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
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
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;
import static org.zstack.storage.volume.DeleteVolumeGcGlobalProperty.DELETE_VOLUME_GC;


@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DeleteVolumeGcExtension implements Component {
    protected static final CLogger logger = Utils.getLogger(DeleteVolumeGcExtension.class);

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
        if (DELETE_VOLUME_GC) {
            thdf.submitTimerTask(this::upgrade, TimeUnit.MINUTES, 5);
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

    boolean cephDeleteVolumeGC(){
        logger.debug("DeleteVolumeGcExtension---------------------------------------------------------------");
        session = Session.login(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);

//        long count = SQL.New("select GarbageCollectorVO.context from GarbageCollectorVO vo " +
//                        "where vo.runnerClass = :runnerClass and vo.status := status", Long.class)
//                .param("runnerClass", CephDeleteVolumeGC.class)
//                .param("status", GCStatus.Idle)
//                .find();
//
//        logger.debug(String.format("%s",count));
//        if (count == 0) {
//            return false;
//        }
//        SQL.New("select GarbageCollectorVO.context, id from GarbageCollectorVO vo " +
//                        "where vo.runnerClass = :runnerClass and vo.status := status", String.class)
//                .param("runnerClass", DeleteVolumeGC.class)
//                .param("status", GCStatus.Idle.toString())
//                .limit(500).paginate(count, (List<String> vids) -> vids.forEach(vid -> {
//
//                if (!Q.New(GarbageCollectorVO.class)
//                        .eq(GarbageCollectorVO_.runnerClass, vid)
//                        .isExists()) {
//                    IAM2VirtualIDOrganizationRefVO refVO = new IAM2VirtualIDOrganizationRefVO();
//                    refVO.setVirtualIDUuid(vid);
//                    refVO.setOrganizationUuid(IAM2Constant.INITIAL_ORGANIZATION_DEFAULT_UUID);
//                    dbf.persist(refVO);
//                }
//
//                SQL.New(GarbageCollectorVO.class).delete();
//                }));
        return true;
    }

    void smpDeleteVolumeGC(){}
    void aliyunEbsDeleteVolumeGC(){}
    void aliyunNasDeleteVolumeGC(){}
    void sharedBlockDeleteVolumeGC(){}
    void nfsDeleteVolumeGC(){}
    void miniStorageDeleteVolumeGC(){}
    void vmwarePrimaryStorageDeleteVolumeGC(){}
}

