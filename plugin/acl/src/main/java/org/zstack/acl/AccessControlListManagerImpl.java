package org.zstack.acl;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.acl.APIAddAccessControlListEntryEvent;
import org.zstack.header.acl.APIAddAccessControlListEntryMsg;
import org.zstack.header.acl.APIAddAccessControlListRedirectRuleMsg;
import org.zstack.header.acl.APIChangeAccessControlListRedirectRuleEvent;
import org.zstack.header.acl.APIChangeAccessControlListRedirectRuleMsg;
import org.zstack.header.acl.APICreateAccessControlListEvent;
import org.zstack.header.acl.APICreateAccessControlListMsg;
import org.zstack.header.acl.APIDeleteAccessControlListEvent;
import org.zstack.header.acl.APIDeleteAccessControlListMsg;
import org.zstack.header.acl.APIRemoveAccessControlListEntryEvent;
import org.zstack.header.acl.APIRemoveAccessControlListEntryMsg;
import org.zstack.header.acl.APIUpdateAccessControlListEvent;
import org.zstack.header.acl.APIUpdateAccessControlListMsg;
import org.zstack.header.acl.AccessControlListConstants;
import org.zstack.header.acl.AccessControlListEntryInventory;
import org.zstack.header.acl.AccessControlListEntryVO;
import org.zstack.header.acl.AccessControlListEntryVO_;
import org.zstack.header.acl.AccessControlListInventory;
import org.zstack.header.acl.AccessControlListVO;
import org.zstack.header.acl.AccessControlListVO_;
import org.zstack.header.acl.AclEntryType;
import org.zstack.header.acl.DeleteAccessControlListMsg;
import org.zstack.header.acl.DeleteAccessControlListReply;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.identity.AccountManager;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author: zhanyong.miao
 * @date: 2020-03-12
 **/
public class AccessControlListManagerImpl extends AbstractService implements AccessControlListManager, Component {
    private static final CLogger logger = Utils.getLogger(AccessControlListManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    protected CascadeFacade casf;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateAccessControlListMsg) {
            handle((APICreateAccessControlListMsg) msg);
        } else if (msg instanceof APIUpdateAccessControlListMsg) {
            handle((APIUpdateAccessControlListMsg) msg);
        } else if (msg instanceof APIDeleteAccessControlListMsg) {
            handle((APIDeleteAccessControlListMsg) msg);
        } else if (msg instanceof APIAddAccessControlListEntryMsg) {
            handle((APIAddAccessControlListEntryMsg) msg);
        } else if (msg instanceof APIChangeAccessControlListRedirectRuleMsg) {
            handle((APIChangeAccessControlListRedirectRuleMsg)msg);
        } else if (msg instanceof APIAddAccessControlListRedirectRuleMsg) {
            handle((APIAddAccessControlListRedirectRuleMsg) msg);
        } else if (msg instanceof APIRemoveAccessControlListEntryMsg) {
            handle((APIRemoveAccessControlListEntryMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIChangeAccessControlListRedirectRuleMsg msg) {
        APIChangeAccessControlListRedirectRuleEvent evt = new APIChangeAccessControlListRedirectRuleEvent(msg.getId());
        AccessControlListEntryVO entryVO = Q.New(AccessControlListEntryVO.class).eq(AccessControlListEntryVO_.uuid, msg.getUuid()).find();
        if (!StringUtils.equals(msg.getName(), entryVO.getName())) {
            entryVO.setName(msg.getName());
            dbf.update(entryVO);
        }
        evt.setInventory(AccessControlListEntryInventory.valueOf(entryVO));
        bus.publish(evt);
    }

    private void handle(APICreateAccessControlListMsg msg) {
        APICreateAccessControlListEvent evt = new APICreateAccessControlListEvent(msg.getId());
        AccessControlListVO vo = new AccessControlListVO();
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setIpVersion(msg.getIpVersion());
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setAccountUuid(msg.getSession().getAccountUuid());
        new SQLBatch() {
            @Override
            protected void scripts() {
                persist(vo);
                reload(vo);
                tagMgr.createTagsFromAPICreateMessage(msg, vo.getUuid(), AccessControlListVO.class.getSimpleName());
            }
        }.execute();
        evt.setInventory(vo.toInventory());
        bus.publish(evt);
    }

    private void handle(APIUpdateAccessControlListMsg msg) {
        AccessControlListVO vo = dbf.findByUuid(msg.getUuid(), AccessControlListVO.class);
        boolean update = false;
        if (msg.getName() != null) {
            vo.setName(msg.getName());
            update = true;
        }
        if (msg.getDescription() != null) {
            vo.setDescription(msg.getDescription());
            update = true;
        }
        if (update) {
            vo = dbf.updateAndRefresh(vo);
        }

        APIUpdateAccessControlListEvent event = new APIUpdateAccessControlListEvent(msg.getId());
        event.setInventory(vo.toInventory());
        bus.publish(event);
    }

    private void handle(APIDeleteAccessControlListMsg msg) {
        APIDeleteAccessControlListEvent evt = new APIDeleteAccessControlListEvent(msg.getId());
        deleteAccessControlList(msg.getUuid(), msg.getDeletionMode(), new Completion(msg) {
            @Override
            public void success() {
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void handle(APIAddAccessControlListEntryMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("operate-acl-%s", msg.getAclUuid());
            }

            @Override
            public void run(final SyncTaskChain chain) {
                APIAddAccessControlListEntryEvent evt = new APIAddAccessControlListEntryEvent(msg.getId());
                AccessControlListVO aclVO = dbf.findByUuid(msg.getAclUuid(), AccessControlListVO.class);
                AccessControlListEntryVO entry = new AccessControlListEntryVO();
                entry.setType(AclEntryType.IpEntry.toString());
                entry.setUuid(Platform.getUuid());
                entry.setAclUuid(msg.getAclUuid());
                entry.setIpEntries(msg.getEntries());
                entry.setDescription(msg.getDescription());

                final AccessControlListEntryVO fentry = entry;
                CollectionUtils.safeForEach(pluginRgty.getExtensionList(RefreshAccessControlListExtensionPoint.class),
                        new ForEachFunction<RefreshAccessControlListExtensionPoint>() {
                            @Override
                            public void run(RefreshAccessControlListExtensionPoint ext) {
                                logger.debug(String.format("execute before add acl ip entry extension point %s", ext));
                                ext.beforeAddIpEntry(aclVO.toInventory(), fentry.toInventory());
                            }
                        });

                entry = dbf.persistAndRefresh(entry);
                /*apply acl*/
                CollectionUtils.safeForEach(pluginRgty.getExtensionList(RefreshAccessControlListExtensionPoint.class),
                        new ForEachFunction<RefreshAccessControlListExtensionPoint>() {
                            @Override
                            public void run(RefreshAccessControlListExtensionPoint ext) {
                                logger.debug(String.format("execute after add acl ip entry extension point %s", ext));
                                ext.afterAddIpEntry(aclVO.toInventory(), fentry.toInventory());
                            }
                        });

                evt.setInventory(entry.toInventory());
                bus.publish(evt);
                chain.next();
            }

            @Override
            public String getName() {
                return "add-acl-ip-entry";
            }
        });

    }

    private void handle(APIAddAccessControlListRedirectRuleMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("operate-acl-%s", msg.getAclUuid());
            }

            @Override
            public void run(final SyncTaskChain chain) {
                APIAddAccessControlListEntryEvent evt = new APIAddAccessControlListEntryEvent(msg.getId());
                AccessControlListVO aclVO = dbf.findByUuid(msg.getAclUuid(), AccessControlListVO.class);
                AccessControlListEntryVO entry = new AccessControlListEntryVO();
                entry.setName(msg.getName());
                entry.setUuid(Platform.getUuid());
                entry.setAclUuid(msg.getAclUuid());
                entry.setType(AclEntryType.RedirectRule.toString());
                entry.setCriterion(msg.getCriterion());
                entry.setMatchMethod(msg.getMatchMethod());
                entry.setDomain(msg.getDomain());
                entry.setUrl(msg.getUrl());
                entry.setDescription(msg.getDescription());
                entry.setRedirectPort(msg.getRedirectPort());
                String redirectRule = "";
                if (entry.getMatchMethod().equals("Url")) {
                   redirectRule = "path_beg -i " + entry.getUrl();
                } else {
                    String domain = entry.getDomain();

                    domain = domain.replace(".", "\\.");
                    if (entry.getCriterion().equals("WildcardMatch")) {
                        domain = domain.replace("*", ".*");
                    }

                    if (entry.getMatchMethod().equals("Domain")) {
                        redirectRule = "hdr_reg(host) -i " + domain;
                    } else {
                        redirectRule = "base_reg -i " + domain + entry.getUrl();
                    }
                }
                entry.setRedirectRule(redirectRule);

                final AccessControlListEntryVO fentry = entry;
                CollectionUtils.safeForEach(pluginRgty.getExtensionList(RefreshAccessControlListExtensionPoint.class),
                        new ForEachFunction<RefreshAccessControlListExtensionPoint>() {
                            @Override
                            public void run(RefreshAccessControlListExtensionPoint ext) {
                                logger.debug(String.format("execute before add acl ip entry extension point %s", ext));
                                ext.beforeAddIpEntry(aclVO.toInventory(), fentry.toInventory());
                            }
                        });

                entry = dbf.persistAndRefresh(entry);
                /*apply acl*/
                CollectionUtils.safeForEach(pluginRgty.getExtensionList(RefreshAccessControlListExtensionPoint.class),
                        new ForEachFunction<RefreshAccessControlListExtensionPoint>() {
                            @Override
                            public void run(RefreshAccessControlListExtensionPoint ext) {
                                logger.debug(String.format("execute after add acl ip entry extension point %s", ext));
                                ext.afterAddIpEntry(aclVO.toInventory(), fentry.toInventory());
                            }
                        });

                evt.setInventory(entry.toInventory());
                bus.publish(evt);
                chain.next();
            }

            @Override
            public String getName() {
                return "add-acl-ip-entry";
            }
        });
    }

    private void handle(APIRemoveAccessControlListEntryMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("operate-acl-%s", msg.getAclUuid());
            }

            @Override
            public void run(final SyncTaskChain chain) {
                APIRemoveAccessControlListEntryEvent evt = new APIRemoveAccessControlListEntryEvent(msg.getId());
                AccessControlListVO aclVO = dbf.findByUuid(msg.getAclUuid(), AccessControlListVO.class);
                AccessControlListEntryVO entry = dbf.findByUuid(msg.getUuid(), AccessControlListEntryVO.class);

                CollectionUtils.safeForEach(pluginRgty.getExtensionList(RefreshAccessControlListExtensionPoint.class),
                        new ForEachFunction<RefreshAccessControlListExtensionPoint>() {
                            @Override
                            public void run(RefreshAccessControlListExtensionPoint ext) {
                                logger.debug(String.format("execute before del acl ip entry extension point %s", ext));
                                ext.beforeDeleteIpEntry(aclVO.toInventory(), entry.toInventory());
                            }
                        });

                new SQLBatch() {
                    @Override
                    protected void scripts() {
                        remove(entry);
                    }
                }.execute();
                /*apply acl*/
                CollectionUtils.safeForEach(pluginRgty.getExtensionList(RefreshAccessControlListExtensionPoint.class),
                        new ForEachFunction<RefreshAccessControlListExtensionPoint>() {
                            @Override
                            public void run(RefreshAccessControlListExtensionPoint ext) {
                                logger.debug(String.format("execute after del acl ip entry extension point %s", ext));
                                ext.afterDeleteIpEntry(aclVO.toInventory(), entry.toInventory());
                            }
                        });

                bus.publish(evt);
                chain.next();
            }

            @Override
            public String getName() {
                return "remove-acl-ip-entry";
            }
        });
    }

    protected void handleLocalMessage(Message msg) {
        if (msg instanceof DeleteAccessControlListMsg) {
            handle((DeleteAccessControlListMsg)msg);
        }
        bus.dealWithUnknownMessage(msg);
    }

    private void handle(DeleteAccessControlListMsg msg) {
        deleteAccessControlList(msg.getUuid(), msg.isForceDelete() ? APIDeleteMessage.DeletionMode.Enforcing : APIDeleteMessage.DeletionMode.Permissive, new Completion(msg) {
            @Override
            public void success() {
                DeleteAccessControlListReply reply = new DeleteAccessControlListReply();
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                DeleteAccessControlListReply reply = new DeleteAccessControlListReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void deleteAccessControlList(String uuid, APIDeleteMessage.DeletionMode mode, Completion completion) {
        if (uuid == null) {
            completion.success();
            return;
        }
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return String.format("operate-acl-%s", uuid);
            }

            @Override
            public void run(final SyncTaskChain chain) {
                AccessControlListVO aclVO = Q.New(AccessControlListVO.class).eq(AccessControlListVO_.uuid, uuid).find();
                if (aclVO == null) {
                    completion.success();
                    chain.next();
                    return;
                }

                final String issuer = AccessControlListVO.class.getSimpleName();
                final List<AccessControlListInventory> ctx = Arrays.asList(AccessControlListInventory.valueOf(aclVO));
                FlowChain fchain = FlowChainBuilder.newSimpleFlowChain();
                fchain.setName(String.format("delete-acl-%s", uuid));

                if (mode == APIDeleteMessage.DeletionMode.Permissive) {
                    // DEBT: NoRollbackFlow — in getSyncSignature
                    fchain.then(new NoRollbackFlow() {
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
                    // DEBT: NoRollbackFlow — in getSyncSignature
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
                    // DEBT: NoRollbackFlow — reason TBD
                    fchain.then(new NoRollbackFlow() {
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
                fchain.done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        // Call extensions before deleting entries
                        List<AccessControlListEntryVO> entries = Q.New(AccessControlListEntryVO.class)
                                .eq(AccessControlListEntryVO_.aclUuid, aclVO.getUuid()).list();

                        entries.forEach(entry -> {
                            CollectionUtils.safeForEach(pluginRgty.getExtensionList(RefreshAccessControlListExtensionPoint.class),
                                ext -> {
                                    logger.debug(String.format("execute before del acl ip entry extension point %s", ext));
                                    ext.beforeDeleteIpEntry(aclVO.toInventory(), entry.toInventory());
                                });
                        });

                        new SQLBatch() {
                            @Override
                            protected void scripts() {
                                List<AccessControlListEntryVO> entrys = Q.New(AccessControlListEntryVO.class)
                                        .eq(AccessControlListEntryVO_.aclUuid, aclVO.getUuid()).list();
                                if (!entrys.isEmpty()) {
                                    entrys.forEach( entry-> remove(entry));
                                }
                                // Call extensions after deleting entries
                                entries.forEach(entry -> {
                                    CollectionUtils.safeForEach(pluginRgty.getExtensionList(RefreshAccessControlListExtensionPoint.class),
                                        ext -> {
                                            logger.debug(String.format("execute after del acl ip entry extension point %s", ext));
                                            ext.afterDeleteIpEntry(aclVO.toInventory(), entry.toInventory());
                                        });
                                });
                                remove(aclVO);
                            }
                        }.execute();
                        completion.success();
                        chain.next();
                    }
                }).error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                        chain.next();
                    }
                }).start();
            }

            @Override
            public String getName() {
                return "del-acl";
            }
        });
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(AccessControlListConstants.SERVICE_ID);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
