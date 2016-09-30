package org.zstack.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.ConfigurationConstant;
import org.zstack.header.configuration.DiskOfferingDeletionMsg;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.core.Completion;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 */
public class DiskOfferingCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(DiskOfferingCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    private static final String NAME = DiskOfferingVO.class.getSimpleName();


    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_CHECK_CODE)) {
            handleDeletionCheck(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE, CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleDeletion(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_CLEANUP_CODE)) {
            handleDeletionCleanup(action, completion);
        } else {
            completion.success();
        }

    }

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        deleteDiskOfferingNotReferredByVolume();
        completion.success();
    }

    private void deleteDiskOfferingNotReferredByVolume() {
        String sql = "select d.uuid from DiskOfferingEO d where d.deleted is not null and d.uuid not in (select v.diskOfferingUuid from VolumeVO v where v.diskOfferingUuid is not null)";
        dbf.hardDeleteCollectionSelectedBySQL(sql, DiskOfferingVO.class);
    }

    private void handleDeletion(final CascadeAction action, final Completion completion) {
        final List<DiskOfferingInventory> doinvs = diskOfferingFromAction(action);
        if (doinvs == null || doinvs.isEmpty()) {
            completion.success();
            return;
        }

        List<DiskOfferingDeletionMsg> msgs = new ArrayList<DiskOfferingDeletionMsg>();
        for (DiskOfferingInventory doinv : doinvs) {
            DiskOfferingDeletionMsg msg = new DiskOfferingDeletionMsg();
            msg.setDiskOfferingUuid(doinv.getUuid());
            msg.setForceDelete(action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE));
            bus.makeTargetServiceIdByResourceUuid(msg, ConfigurationConstant.SERVICE_ID, doinv.getUuid());
            msgs.add(msg);
        }

        bus.send(msgs, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                if (!action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
                    for (MessageReply r : replies) {
                        if (!r.isSuccess()) {
                            completion.fail(r.getError());
                            return;
                        }
                    }
                }

                completion.success();
            }
        });
    }

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        completion.success();
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(AccountVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    private List<DiskOfferingInventory> diskOfferingFromAction(CascadeAction action) {
        if (NAME.equals(action.getParentIssuer())) {
            return action.getParentIssuerContext();
        } else if (AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return getForTheAccount((List<AccountInventory>)action.getParentIssuerContext());
        }

        throw new CloudRuntimeException("should not be here");
    }

    @Transactional(readOnly = true)
    private List<DiskOfferingInventory> getForTheAccount(List<AccountInventory> invs) {
        List<String> accountUuids = CollectionUtils.transformToList(invs, new Function<String, AccountInventory>() {
            @Override
            public String call(AccountInventory arg) {
                return arg.getUuid();
            }
        });

        String sql = "select d from DiskOfferingVO d, AccountResourceRefVO r where d.uuid = r.resourceUuid and" +
                " r.resourceType = :rtype and r.accountUuid in (:auuids)";
        TypedQuery<DiskOfferingVO> q = dbf.getEntityManager().createQuery(sql, DiskOfferingVO.class);
        q.setParameter("rtype", DiskOfferingVO.class.getSimpleName());
        q.setParameter("auuids", accountUuids);
        List<DiskOfferingVO> vos = q.getResultList();
        return DiskOfferingInventory.valueOf(vos);
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<DiskOfferingInventory> ctx = diskOfferingFromAction(action);
            if (ctx != null && !ctx.isEmpty()) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(ctx);
            }
        }

        return null;
    }
}
