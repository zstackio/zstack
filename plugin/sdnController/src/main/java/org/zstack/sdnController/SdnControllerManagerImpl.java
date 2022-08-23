package org.zstack.sdnController;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.sdnController.header.*;
import org.zstack.tag.TagManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

public class SdnControllerManagerImpl extends AbstractService implements SdnControllerManager {
    private static final CLogger logger = Utils.getLogger(SdnControllerManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private CascadeFacade casf;
    @Autowired
    private ThreadFacade thdf;

    private Map<String, SdnControllerFactory> sdnControllerFactories = Collections.synchronizedMap(new HashMap<String, SdnControllerFactory>());

    @Override
    public int getSyncLevel() {
        return super.getSyncLevel();
    }

    @Override
    public List<String> getAliasIds() {
        return super.getAliasIds();
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIAddSdnControllerMsg) {
            handle((APIAddSdnControllerMsg) msg);
        } else if (msg instanceof APIRemoveSdnControllerMsg) {
            handle((APIRemoveSdnControllerMsg) msg);
        } else if (msg instanceof APIUpdateSdnControllerMsg) {
            handle((APIUpdateSdnControllerMsg) msg);
        } else if (msg instanceof SdnControllerDeletionMsg) {
            handle((SdnControllerDeletionMsg) msg);
        }
    }

    private void doDeletionSdnController(SdnControllerDeletionMsg msg, Completion completion) {
        SdnControllerVO vo = dbf.findByUuid(msg.getSdnControllerUuid(), SdnControllerVO.class);
        SdnControllerFactory factory = getSdnControllerFactory(vo.getVendorType());

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("sdn-controller-deletion-%s", msg.getSdnControllerUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "detach-hardvxlan-network-of-sdn-controller";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<HardwareL2VxlanNetworkPoolVO> poolVos = Q.New(HardwareL2VxlanNetworkPoolVO.class)
                        .eq(HardwareL2VxlanNetworkPoolVO_.sdnControllerUuid, msg.getSdnControllerUuid()).list();
                new While<>(poolVos).all((pool, wcomp) -> {
                    DeleteL2NetworkMsg msg = new DeleteL2NetworkMsg();
                    msg.setUuid(pool.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(msg, L2NetworkConstant.SERVICE_ID, pool.getUuid());
                    bus.send(msg, new CloudBusCallBack(wcomp) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.info(String.format("delete hardware vxpool[uuid:%s] failed, reason:%s", pool.getUuid(), reply.getError().getDetails()));
                            }
                            wcomp.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "delete-sdn-controller";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                dbf.removeByPrimaryKey(msg.getSdnControllerUuid(), SdnControllerVO.class);
                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                factory.deleteSdnController(vo, msg, new Completion(msg) {
                    @Override
                    public void success() {
                       trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                       trigger.fail(errorCode);
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void handle(SdnControllerDeletionMsg msg) {
        SdnControllerDeletionReply reply = new SdnControllerDeletionReply();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("sdn-controller-%s", msg.getSdnControllerUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                doDeletionSdnController(msg, new Completion(msg) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("attach-sdn-controller-%s", msg.getSdnControllerUuid());
            }
        });
    }

    private void handle(APIAddSdnControllerMsg msg) {
        APIAddSdnControllerEvent event = new APIAddSdnControllerEvent(msg.getId());

        SdnControllerFactory factory = getSdnControllerFactory(msg.getVendorType());
        SdnControllerVO vo = new SdnControllerVO();
        vo.setVendorType(msg.getVendorType());
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setIp(msg.getIp());
        vo.setUsername(msg.getUserName());
        vo.setPassword(msg.getPassword());
        vo.setAccountUuid(msg.getSession().getAccountUuid());
        factory.createSdnController(vo, msg, new Completion(msg) {
            @Override
            public void success() {
                tagMgr.createTagsFromAPICreateMessage(msg, vo.getUuid(), SdnControllerVO.class.getSimpleName());
                event.setInventory(SdnControllerInventory.valueOf(dbf.findByUuid(vo.getUuid(), SdnControllerVO.class)));
                bus.publish(event);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                event.setError(errorCode);
                bus.publish(event);
            }
        });
    }

    private void handle(APIRemoveSdnControllerMsg msg) {
        APIRemoveSdnControllerEvent event = new APIRemoveSdnControllerEvent(msg.getId());

        final String issuer = SdnControllerVO.class.getSimpleName();
        SdnControllerVO vo = dbf.findByUuid(msg.getUuid(), SdnControllerVO.class);
        final List<SdnControllerInventory> ctx = asList(SdnControllerInventory.valueOf(vo));
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-sdn-controller-%s-name-%s", msg.getUuid(), vo.getName()));
        if (msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Permissive) {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_CHECK_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            }).then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_DELETE_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            });
        } else {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_FORCE_DELETE_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            });
        }

        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                bus.publish(event);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                event.setError(errCode);
                bus.publish(event);
            }
        }).start();
    }

    private void handle(APIUpdateSdnControllerMsg msg) {
        APIUpdateSdnControllerEvent event = new APIUpdateSdnControllerEvent(msg.getId());
        SdnControllerVO vo = dbf.findByUuid(msg.getUuid(), SdnControllerVO.class);
        Boolean changed = false;

        if (msg.getName() != null && !msg.getName().equals(vo.getName())) {
            vo.setName(msg.getName());
            changed = true;
        }

        if (msg.getDescription() != null && !msg.getDescription().equals(vo.getDescription())) {
            vo.setDescription(msg.getDescription());
            changed = true;
        }

        if (changed) {
            vo = dbf.updateAndRefresh(vo);
        }

        event.setInventory(SdnControllerInventory.valueOf(vo));
        bus.publish(event);
    }

    @Override
    public SdnControllerFactory getSdnControllerFactory(String type) {
        SdnControllerFactory factory = sdnControllerFactories.get(type);
        if (factory == null) {
            throw new CloudRuntimeException(String.format("Cannot find sdn controller for type(%s)", type));
        }

        return factory;
    }

    @Override
    public SdnController getSdnController(SdnControllerVO sdnControllerVO) {
        SdnControllerFactory factory = getSdnControllerFactory(sdnControllerVO.getVendorType());
        return factory.getSdnController(sdnControllerVO);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SdnControllerConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        for (SdnControllerFactory f : pluginRgty.getExtensionList(SdnControllerFactory.class)) {
            SdnControllerFactory old = sdnControllerFactories.get(f.getVendorType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate SdnControllerFactory[%s, %s] for type[%s]",
                        f.getClass().getName(), old.getClass().getName(), f.getVendorType()));
            }
            sdnControllerFactories.put(f.getVendorType().toString(), f);
        }

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
