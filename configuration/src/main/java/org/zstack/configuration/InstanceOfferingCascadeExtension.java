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
import org.zstack.header.configuration.InstanceOfferingDeletionMsg;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.core.Completion;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 */
public class InstanceOfferingCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(InstanceOfferingCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    private static final String NAME = InstanceOfferingVO.class.getSimpleName();


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
        deleteInstanceOfferingEONotReferredByVm();
        completion.success();
    }

    @Transactional
    private void deleteInstanceOfferingEONotReferredByVm() {
        String sql = "delete from InstanceOfferingEO i where i.deleted is not null and i.uuid not in (select vm.instanceOfferingUuid from VmInstanceVO vm where vm.instanceOfferingUuid is not null)";
        Query q = dbf.getEntityManager().createQuery(sql);
        q.executeUpdate();
    }

    private void handleDeletion(final CascadeAction action, final Completion completion) {
        final List<InstanceOfferingInventory> ioinvs = instanceOfferingFromAction(action);
        if (ioinvs == null) {
            completion.success();
            return;
        }

        List<InstanceOfferingDeletionMsg> msgs = new ArrayList<InstanceOfferingDeletionMsg>();
        for (InstanceOfferingInventory ioinv : ioinvs) {
            InstanceOfferingDeletionMsg msg = new InstanceOfferingDeletionMsg();
            msg.setInstanceOfferingUuid(ioinv.getUuid());
            msg.setForceDelete(action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE));
            bus.makeTargetServiceIdByResourceUuid(msg, ConfigurationConstant.SERVICE_ID, ioinv.getUuid());
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
        return Arrays.asList();
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    private List<InstanceOfferingInventory> instanceOfferingFromAction(CascadeAction action) {
        return action.getParentIssuerContext();
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            return action;
        }

        return null;
    }
}
