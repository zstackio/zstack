package org.zstack.storage.volume;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.Component;
import org.zstack.header.storage.primary.PrimaryStorageCanonicalEvent;
import org.zstack.header.storage.primary.PrimaryStorageCanonicalEvent.PrimaryStorageStatusChangedData;
import org.zstack.header.storage.primary.PrimaryStorageStatus;
import org.zstack.header.volume.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import java.util.List;
import java.util.Map;

/**
 * Created by xing5 on 2016/5/25.
 */
public class VolumeUpgradeExtension implements Component {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private EventFacade evtf;
    @Autowired
    private CloudBus bus;

    @Override
    public boolean start() {
        if (!CoreGlobalProperty.IS_UPGRADE_START) {
            return true;
        }

        String dbVersion = dbf.getDbVersion();
        if ("1.3".equals(dbVersion)) {
            handle1_3();
        }

        return true;
    }

    private void handle1_3() {
        evtf.on(PrimaryStorageCanonicalEvent.PRIMARY_STORAGE_STATUS_CHANGED_PATH, new EventCallback() {
            @Override
            public void run(Map tokens, Object data) {
                if (!evtf.isFromThisManagementNode(tokens)) {
                    return;
                }

                PrimaryStorageStatusChangedData d = (PrimaryStorageStatusChangedData) data;

                if (!PrimaryStorageStatus.Connected.toString().equals(d.getNewStatus())) {
                    return;
                }

                SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
                q.add(VolumeVO_.primaryStorageUuid, Op.EQ, d.getPrimaryStorageUuid());
                q.add(VolumeVO_.status, Op.EQ, VolumeStatus.Ready);
                q.add(VolumeVO_.actualSize, Op.NULL);
                List<VolumeVO> vols = q.list();

                if (vols.isEmpty()) {
                    return;
                }

                List<SyncVolumeSizeMsg> msgs = CollectionUtils.transformToList(vols, new Function<SyncVolumeSizeMsg, VolumeVO>() {
                    @Override
                    public SyncVolumeSizeMsg call(VolumeVO arg) {
                        SyncVolumeSizeMsg msg = new SyncVolumeSizeMsg();
                        msg.setVolumeUuid(arg.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, arg.getUuid());
                        return msg;
                    }
                });

                bus.send(msgs);
            }
        });
    }

    @Override
    public boolean stop() {
        return true;
    }
}
