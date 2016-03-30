package org.zstack.storage.primary.smp;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.storage.primary.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by xing5 on 2016/3/26.
 */
public class SMPPrimaryStorageFactory implements PrimaryStorageFactory {
    private static final CLogger logger = Utils.getLogger(SMPPrimaryStorageFactory.class);

    public static final PrimaryStorageType type = new PrimaryStorageType(SMPConstants.SMP_TYPE);

    static {
        type.setSupportPingStorageGateway(true);
        type.setSupportHeartbeatFile(true);
    }

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public PrimaryStorageType getPrimaryStorageType() {
        return type;
    }

    @Override
    public PrimaryStorageInventory createPrimaryStorage(PrimaryStorageVO vo, APIAddPrimaryStorageMsg msg) {
        vo.setMountPath(vo.getUrl());
        vo.setType(SMPConstants.SMP_TYPE);
        vo = dbf.persistAndRefresh(vo);
        return PrimaryStorageInventory.valueOf(vo);
    }

    @Override
    public PrimaryStorage getPrimaryStorage(PrimaryStorageVO vo) {
        return new SMPPrimaryStorageBase(vo);
    }

    @Override
    public PrimaryStorageInventory getInventory(String uuid) {
        return PrimaryStorageInventory.valueOf(dbf.findByUuid(uuid, PrimaryStorageVO.class));
    }
}
