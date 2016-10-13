package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.storage.primary.*;
import org.zstack.header.zone.ZoneVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import java.util.Arrays;
import java.util.List;

/**
 * Created by miao on 16-10-13.
 */
public class LocalStorageCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(LocalStorageCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    private static final String NAME = PrimaryStorageClusterRefVO.class.getSimpleName();


    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(PrimaryStorageConstant.PRIMARY_STORAGE_DETACH_CODE)) {
            handlePrimaryStorageDetach(action, completion);
        } else {
            completion.success();
        }
    }

    @Transactional
    private void handlePrimaryStorageDetach(CascadeAction action, Completion completion) {
        List<PrimaryStorageDetachStruct> structs = action.getParentIssuerContext();
        for (PrimaryStorageDetachStruct primaryStorageDetachStruct : structs) {
            String psUuid = primaryStorageDetachStruct.getPrimaryStorageUuid();
            SimpleQuery<PrimaryStorageVO> sq = dbf.createQuery(PrimaryStorageVO.class);
            sq.add(PrimaryStorageVO_.uuid, SimpleQuery.Op.EQ, psUuid);
            PrimaryStorageVO psVO = sq.find();

            if (!psVO.getType().equals(LocalStorageConstants.LOCAL_STORAGE_TYPE)) {
                completion.success();
                return;
            }

            {
                String sql = "delete from LocalStorageHostRefVO where primaryStorageUuid = :primaryStorageUuid";
                Query q = dbf.getEntityManager().createQuery(sql);
                q.setParameter("primaryStorageUuid", psUuid);
                q.executeUpdate();
            }

            RecalculatePrimaryStorageCapacityMsg rmsg = new RecalculatePrimaryStorageCapacityMsg();
            rmsg.setPrimaryStorageUuid(psUuid);
            bus.makeLocalServiceId(rmsg, PrimaryStorageConstant.SERVICE_ID);
            bus.send(rmsg);
        }
        completion.success();
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(PrimaryStorageVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        return null;
    }
}
