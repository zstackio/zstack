package org.zstack.acl;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
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
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.acl.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.identity.AccountManager;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;

import java.util.List;

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

    private void handle(APIDeleteAccessControlListMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("operate-acl-%s", msg.getUuid());
            }

            @Override
            public void run(final SyncTaskChain chain) {
                APIDeleteAccessControlListEvent evt = new APIDeleteAccessControlListEvent(msg.getId());
                AccessControlListVO aclVO = dbf.findByUuid(msg.getUuid(), AccessControlListVO.class);

                /*check if the acl group used*/
                CollectionUtils.safeForEach(pluginRgty.getExtensionList(RefreshAccessControlListExtensionPoint.class),
                        new ForEachFunction<RefreshAccessControlListExtensionPoint>() {
                            @Override
                            public void run(RefreshAccessControlListExtensionPoint ext) {
                                logger.debug(String.format("execute before del acl group extension point %s", ext));
                                ext.beforeDeleteAcl(aclVO.toInventory());
                            }
                        });
                new SQLBatch() {
                    @Override
                    protected void scripts() {
                        List<AccessControlListEntryVO> entrys = Q.New(AccessControlListEntryVO.class)
                                .eq(AccessControlListEntryVO_.aclUuid, aclVO.getUuid()).list();
                        if (!entrys.isEmpty()) {
                            entrys.forEach( entry-> remove(entry));
                        }
                        remove(aclVO);
                    }
                }.execute();
                CollectionUtils.safeForEach(pluginRgty.getExtensionList(RefreshAccessControlListExtensionPoint.class),
                        new ForEachFunction<RefreshAccessControlListExtensionPoint>() {
                            @Override
                            public void run(RefreshAccessControlListExtensionPoint ext) {
                                logger.debug(String.format("execute after del acl group extension point %s", ext));
                                ext.afterDeleteAcl(aclVO.toInventory());
                            }
                        });
                bus.publish(evt);
                chain.next();
            }

            @Override
            public String getName() {
                return "del-acl";
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
        bus.dealWithUnknownMessage(msg);
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
