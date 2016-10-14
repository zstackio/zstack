package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.asyncbatch.AsyncBatchRunner;
import org.zstack.core.asyncbatch.LoopAsyncBatch;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.*;

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
        FutureCompletion futureCompletion = new FutureCompletion();
        List<ErrorCode> errorCodes = new ArrayList<>();

        new LoopAsyncBatch<PrimaryStorageDetachStruct>(completion, futureCompletion) {
            @Override
            protected Collection<PrimaryStorageDetachStruct> collect() {
                return structs;
            }

            @Override
            protected AsyncBatchRunner forEach(PrimaryStorageDetachStruct primaryStorageDetachStruct) {
                return new AsyncBatchRunner() {
                    @Override
                    public void run(NoErrorCompletion completion) {
                        Completion comp = new Completion() {
                            @Override
                            public void success() {
                                completion.done();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                synchronized (errorCodes) {
                                    errorCodes.add(errorCode);
                                    completion.done();
                                }
                            }
                        };

                        String psUuid = primaryStorageDetachStruct.getPrimaryStorageUuid();
                        PrimaryStorageVO psVO;
                        {
                            SimpleQuery<PrimaryStorageVO> sq = dbf.createQuery(PrimaryStorageVO.class);
                            sq.add(PrimaryStorageVO_.uuid, SimpleQuery.Op.EQ, psUuid);
                            psVO = sq.find();
                        }

                        if (!psVO.getType().equals(LocalStorageConstants.LOCAL_STORAGE_TYPE)) {
                            completion.done();
                            return;
                        }


                        FlowChain chain = FlowChainBuilder.newShareFlowChain();
                        chain.setName(String.format("detach-all-hosts-from-primary-storage-%s", psUuid));
                        chain.then(new ShareFlow() {

                            @Override
                            public void setup() {
                                flow(new NoRollbackFlow() {
                                    String __name__ = String.format("remove-all-hosts-from-primary-storage-%s", psUuid);

                                    @Override
                                    public void run(FlowTrigger trigger, Map data) {
                                        SimpleQuery<LocalStorageHostRefVO> sq = dbf.createQuery(LocalStorageHostRefVO.class);
                                        sq.select(LocalStorageHostRefVO_.hostUuid);
                                        sq.add(LocalStorageHostRefVO_.primaryStorageUuid, SimpleQuery.Op.EQ, psUuid);
                                        List<String> hostUuids = sq.list();
                                        List<RemoveHostFromLocalStorageMsg> msgs = CollectionUtils.transformToList(hostUuids,
                                                new Function<RemoveHostFromLocalStorageMsg, String>() {
                                                    @Override
                                                    public RemoveHostFromLocalStorageMsg call(String arg) {
                                                        RemoveHostFromLocalStorageMsg msg = new RemoveHostFromLocalStorageMsg();
                                                        msg.setHostUuid(arg);
                                                        msg.setPrimaryStorageUuid(psUuid);
                                                        return msg;
                                                    }
                                                });
                                        bus.send(msgs, new CloudBusListCallBack(trigger) {
                                            @Override
                                            public void run(List<MessageReply> replies) {
                                                StringBuilder sb = new StringBuilder();
                                                boolean success = true;
                                                for (MessageReply r : replies) {
                                                    if (!r.isSuccess()) {
                                                        RemoveHostFromLocalStorageMsg msg = msgs.get(replies.indexOf(r));
                                                        String err = String.format("\nFailed to remove host[uuid:%s] from primary storage[uuid:%s], %s",
                                                                msg.getHostUuid(), psUuid, r.getError());
                                                        sb.append(err);
                                                        success = false;
                                                    }
                                                }

                                                if (!success) {
                                                    logger.warn(sb.toString());
                                                }

                                                if (success) {
                                                    trigger.next();
                                                } else {
                                                    trigger.fail(errf.stringToOperationError(sb.toString()));
                                                }
                                            }
                                        });
                                    }
                                });

                                done(new FlowDoneHandler(completion) {
                                    @Override
                                    public void handle(Map data) {

                                        RecalculatePrimaryStorageCapacityMsg rmsg = new RecalculatePrimaryStorageCapacityMsg();
                                        rmsg.setPrimaryStorageUuid(psUuid);
                                        bus.makeLocalServiceId(rmsg, PrimaryStorageConstant.SERVICE_ID);
                                        bus.send(rmsg);

                                        completion.done();
                                    }
                                });
                            }
                        }).start();
                    }
                };
            }

            @Override
            protected void done() {
                completion.success();
            }
        }.start();
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
