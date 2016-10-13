package org.zstack.storage.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cascade.*;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.header.zone.ZoneVO;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 */
public class PrimaryStorageCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(PrimaryStorageCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PrimaryStorageExtensionPointEmitter extpEmitter;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    private static final String NAME = PrimaryStorageVO.class.getSimpleName();

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_CHECK_CODE)) {
            handleDeletionCheck(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE, CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleDeletion(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_CLEANUP_CODE)) {
            handleDeletionCleanup(action, completion);
        } else if (action.isActionCode(PrimaryStorageConstant.PRIMARY_STORAGE_DETACH_CODE)) {
            handlePrimaryStorageDetach(action, completion);
        } else {
            completion.success();
        }
    }

    private void handlePrimaryStorageDetach(CascadeAction action, final Completion completion) {
        List<PrimaryStorageDetachStruct> structs = action.getParentIssuerContext();
        List<DetachPrimaryStorageFromClusterMsg> msgs = CollectionUtils.transformToList(structs,
                new Function<DetachPrimaryStorageFromClusterMsg, PrimaryStorageDetachStruct>() {
                    @Override
                    public DetachPrimaryStorageFromClusterMsg call(PrimaryStorageDetachStruct arg) {
                        DetachPrimaryStorageFromClusterMsg msg = new DetachPrimaryStorageFromClusterMsg();
                        msg.setPrimaryStorageUuid(arg.getPrimaryStorageUuid());
                        msg.setClusterUuid(arg.getClusterUuid());
                        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, arg.getPrimaryStorageUuid());
                        return msg;
                    }
                });

        bus.send(msgs, 1, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                for (MessageReply r : replies) {
                    if (!r.isSuccess()) {
                        completion.fail(r.getError());
                        return;
                    }

                    completion.success();
                }
            }
        });
    }

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        dbf.eoCleanup(PrimaryStorageVO.class);
        completion.success();
    }

    private void handleDeletion(final CascadeAction action, final Completion completion) {
        final List<PrimaryStorageInventory> prinvs = primaryStorageInventories(action);
        if (prinvs == null) {
            completion.success();
            return;
        }

        List<PrimaryStorageDeletionMsg> msgs = new ArrayList<>();
        for (PrimaryStorageInventory prinv : prinvs) {
            PrimaryStorageDeletionMsg msg = new PrimaryStorageDeletionMsg();
            msg.setPrimaryStorageUuid(prinv.getUuid());
            msg.setForceDelete(action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE));
            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, prinv.getUuid());
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

                List<String> uuids = new ArrayList<>();
                for (MessageReply r : replies) {
                    PrimaryStorageInventory inv = prinvs.get(replies.indexOf(r));
                    uuids.add(inv.getUuid());
                    logger.debug(String.format("delete primary storage[uuid:%s, name:%s]", inv.getUuid(), inv.getName()));
                }

                dbf.removeByPrimaryKeys(uuids, PrimaryStorageVO.class);
                completion.success();
            }
        });
    }

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        List<PrimaryStorageInventory> prinvs = primaryStorageInventories(action);
        if (prinvs == null) {
            completion.success();
            return;
        }

        try {
            for (PrimaryStorageInventory prinv : prinvs) {
                extpEmitter.preDelete(prinv);
            }

            completion.success();
        } catch (PrimaryStorageException e) {
            completion.fail(errf.throwableToInternalError(e));
        }
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(ZoneVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    private List<PrimaryStorageInventory> primaryStorageInventories(CascadeAction action) {
        List<PrimaryStorageInventory> ret = null;
        if (ZoneVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<String> zuuids = CollectionUtils.transformToList(
                    (List<ZoneInventory>) action.getParentIssuerContext(), new Function<String, ZoneInventory>() {
                        @Override
                        public String call(ZoneInventory arg) {
                            return arg.getUuid();
                        }
                    });

            SimpleQuery<PrimaryStorageVO> q = dbf.createQuery(PrimaryStorageVO.class);
            q.add(PrimaryStorageVO_.zoneUuid, SimpleQuery.Op.IN, zuuids);
            List<PrimaryStorageVO> prvos = q.list();
            if (!prvos.isEmpty()) {
                ret = PrimaryStorageInventory.valueOf(prvos);
            }
        } else if (NAME.equals(action.getParentIssuer())) {
            ret = action.getParentIssuerContext();
        }

        return ret;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<PrimaryStorageInventory> ctx = primaryStorageInventories(action);
            if (ctx != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(ctx);
            }
        } else if (action.isActionCode(PrimaryStorageConstant.PRIMARY_STORAGE_DETACH_CODE)) {
            return action.copy().setParentIssuer(NAME);
        }

        return null;
    }
}
