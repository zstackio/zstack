package org.zstack.compute.zone;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.DbEntityLister;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.tag.AbstractSystemTagOperationJudger;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.tag.SystemTagValidator;
import org.zstack.header.zone.*;
import org.zstack.search.SearchQuery;
import org.zstack.tag.TagManager;
import org.zstack.utils.network.ManagementNetworkIpVersionUtils;
import org.zstack.utils.ObjectUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

import static org.zstack.core.Platform.err;
import static java.util.Arrays.asList;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;


public class ZoneManagerImpl extends AbstractService implements ZoneManager {
    private final static CLogger logger = Utils.getLogger(ZoneManager.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private DbEntityLister dl;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ManagementNetworkIpVersionManagerImpl managementNetworkIpVersionManager;

    private Map<String, ZoneFactory> zoneFactories = Collections.synchronizedMap(new HashMap<String, ZoneFactory>());
    private static final Set<Class> allowedMessageAfterSoftDeletion = new HashSet<Class>();
    private static final String DEFAULT_MANAGEMENT_NETWORK_IP_VERSION_TAG =
            String.format("managementNetwork::ipVersion::%s", ManagementNetworkIpVersionUtils.IPV4);

    static {
        allowedMessageAfterSoftDeletion.add(ZoneDeletionMsg.class);
    }


    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage)msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof ZoneMessage) {
            passThrough((ZoneMessage)msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateZoneMsg) {
            handle((APICreateZoneMsg) msg);
        } else if (msg instanceof ZoneMessage) {
            passThrough((ZoneMessage)msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetZoneMsg msg) {
        APIGetZoneReply reply = new APIGetZoneReply();

        if (msg.getUuid() != null) {
            ZoneVO vo = dbf.findByUuid(msg.getUuid(), ZoneVO.class);
            reply.setInventories(asList(ZoneInventory.valueOf(vo)));
        } else {
            reply.setInventories(ZoneInventory.valueOf(dbf.listAll(ZoneVO.class)));
        }

        bus.reply(msg, reply);
    }

    private void passThrough(ZoneMessage msg) {
        ZoneVO vo = dbf.findByUuid(msg.getZoneUuid(), ZoneVO.class);
        if (vo == null && allowedMessageAfterSoftDeletion.contains(msg.getClass())) {
            ZoneEO eo = dbf.findByUuid(msg.getZoneUuid(), ZoneEO.class);
            vo = ObjectUtils.newAndCopy(eo, ZoneVO.class);
        }

        if (vo == null) {
            ErrorCode err = err(ORG_ZSTACK_COMPUTE_ZONE_10000, SysErrors.RESOURCE_NOT_FOUND, "unable to find zone[uuid:%s], it may have been deleted", msg.getZoneUuid());
            bus.replyErrorByMessageType((Message)msg, err);
            return;
        }

        ZoneFactory factory = this.getZoneFactory(ZoneType.valueOf(vo.getType()));
        Zone zone = factory.getZone(vo);
        zone.handleMessage((Message)msg);
    }

    private ZoneInventory createZoneFromApiMessage(APICreateZoneMsg msg) {
        String zoneType = msg.getType();
        if (zoneType == null) {
            zoneType = BaseZoneFactory.type.toString();
        }
        ZoneFactory factory = this.getZoneFactory(ZoneType.valueOf(zoneType));
        ZoneVO vo = new ZoneVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());

        if (msg.getDefault() != null) {
            vo.setDefault(msg.getDefault());
        } else {
            vo.setDefault(false);
        }

        final ZoneVO finalVO = factory.createZone(vo, msg);
        new SQLBatch() {
            @Override
            protected void scripts() {
                if (msg.getDefault() == null && !q(ZoneVO.class).isExists()) {
                    finalVO.setDefault(true);
                }

                if (finalVO.isDefault()) {
                    sql(ZoneVO.class)
                            .set(ZoneVO_.isDefault, false)
                            .update();
                }

                persist(finalVO);
                reload(finalVO);
            }
        }.execute();

        tagMgr.createTagsFromAPICreateMessage(msg, finalVO.getUuid(), ZoneVO.class.getSimpleName());
        createDefaultManagementNetworkIpVersionTagIfAbsent(finalVO.getUuid());

        return ZoneInventory.valueOf(finalVO);
    }

    private void createDefaultManagementNetworkIpVersionTagIfAbsent(String zoneUuid) {
        if (ZoneSystemTags.MANAGEMENT_NETWORK_IP_VERSION.getTagInventory(zoneUuid) != null) {
            return;
        }

        tagMgr.createNonInherentSystemTag(
                zoneUuid,
                DEFAULT_MANAGEMENT_NETWORK_IP_VERSION_TAG,
                ZoneVO.class.getSimpleName());
    }

    private void createZone(APICreateZoneMsg msg, ReturnValueCompletion<ZoneInventory> completion) {
        if (msg.getDefault() != null && !msg.getDefault()) {
            completion.success(createZoneFromApiMessage(msg));
            return;
        }

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "default-zone-operation-queue";
            }

            @Override
            public void run(SyncTaskChain chain) {
                completion.success(createZoneFromApiMessage(msg));
                chain.next();
            }

            @Override
            public String getName() {
                return "create-zone";
            }
        });
    }

    private void handle(APICreateZoneMsg msg) {
        APICreateZoneEvent evt = new APICreateZoneEvent(msg.getId());
        createZone(msg, new ReturnValueCompletion<ZoneInventory>(msg) {
            @Override
            public void success(ZoneInventory returnValue) {
                evt.setInventory(returnValue);
                logger.debug(String.format("Created zone: %s uuid: %s",
                        evt.getInventory().getName(),
                        evt.getInventory().getUuid())
                );
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(ZoneConstant.SERVICE_ID);
    }

    private void populateExtensions() {
        for (ZoneFactory f : pluginRgty.getExtensionList(ZoneFactory.class)) {
            ZoneFactory old = zoneFactories.get(f.getType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate ZoneFactory[%s, %s] for type[%s]",
                        f.getClass().getName(), old.getClass().getName(), f.getType()));
            }
            zoneFactories.put(f.getType().toString(), f);
        }
    }

    @Override
    public boolean start() {
        populateExtensions();
        ZoneSystemTags.MANAGEMENT_NETWORK_IP_VERSION.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                managementNetworkIpVersionManager.validateZoneIpVersionTagValue(resourceUuid, systemTag);
            }
        });
        ZoneSystemTags.MANAGEMENT_NETWORK_IP_VERSION.installJudger(new AbstractSystemTagOperationJudger() {
            @Override
            public void tagPreCreated(SystemTagInventory tag) {
                managementNetworkIpVersionManager.validateZoneIpVersionTag(
                        tag.getResourceUuid(), tag.getTag());
            }

            @Override
            public void tagPreUpdated(SystemTagInventory old, SystemTagInventory newTag) {
                managementNetworkIpVersionManager.validateZoneIpVersionTag(
                        newTag.getResourceUuid(), newTag.getTag(), old.getUuid());
            }

            @Override
            public void tagPreDeleted(SystemTagInventory tag) {
                managementNetworkIpVersionManager.validateZoneCompatibleWithExistingResources(
                        tag.getResourceUuid(), ManagementNetworkIpVersionUtils.IPV4);
            }
        });
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private ZoneFactory getZoneFactory(ZoneType type) {
        ZoneFactory factory = zoneFactories.get(type.toString());
        if (factory == null) {
            throw new CloudRuntimeException(String.format("No ZoneFactory of type[%s] found", type));
        }
        return factory;
    }
}
