package org.zstack.compute.zone;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.DbEntityLister;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.zone.*;
import org.zstack.search.SearchQuery;
import org.zstack.tag.TagManager;
import org.zstack.utils.ObjectUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

import static java.util.Arrays.asList;


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

    private Map<String, ZoneFactory> zoneFactories = Collections.synchronizedMap(new HashMap<String, ZoneFactory>());
    private static final Set<Class> allowedMessageAfterSoftDeletion = new HashSet<Class>();

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
        } else if (msg instanceof APIListZonesMsg) {
            handle((APIListZonesMsg) msg);
        } else if (msg instanceof APISearchZoneMsg) {
            handle((APISearchZoneMsg) msg);
        } else if (msg instanceof APIGetZoneMsg) {
            handle((APIGetZoneMsg) msg);
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

    private void handle(APISearchZoneMsg msg) {
        SearchQuery<ZoneInventory> query =  SearchQuery.create(msg, ZoneInventory.class);
        String res = query.listAsString();
        APISearchZoneReply reply = new APISearchZoneReply();
        reply.setContent(res);
        bus.reply(msg, reply);
    }

    private void passThrough(ZoneMessage msg) {
        ZoneVO vo = dbf.findByUuid(msg.getZoneUuid(), ZoneVO.class);
        if (vo == null && allowedMessageAfterSoftDeletion.contains(msg.getClass())) {
            ZoneEO eo = dbf.findByUuid(msg.getZoneUuid(), ZoneEO.class);
            vo = ObjectUtils.newAndCopy(eo, ZoneVO.class);
        }

        if (vo == null) {
            ErrorCode err = errf.instantiateErrorCode(SysErrors.RESOURCE_NOT_FOUND, String.format("unable to find zone[uuid:%s], it may have been deleted", msg.getZoneUuid()));
            bus.replyErrorByMessageType((Message)msg, err);
            return;
        }

        ZoneFactory factory = this.getZoneFactory(ZoneType.valueOf(vo.getType()));
        Zone zone = factory.getZone(vo);
        zone.handleMessage((Message)msg);
    }

    private void handle(APIListZonesMsg msg) {
        APIListZonesReply reply = new APIListZonesReply();
        List<ZoneVO> vos = dl.listByApiMessage(msg, ZoneVO.class);
        List<ZoneInventory> invs = ZoneInventory.valueOf(vos);
        reply.setInventories(invs);
        bus.reply(msg, reply);

    }

    private void handle(APICreateZoneMsg msg) {
        String zoneType = msg.getType();
        if (zoneType == null) {
            zoneType = BaseZoneFactory.type.toString();
        }
        ZoneFactory factory = this.getZoneFactory(ZoneType.valueOf(zoneType));
        APICreateZoneEvent evt = new APICreateZoneEvent(msg.getId());
        ZoneVO vo = new ZoneVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo = factory.createZone(vo, msg);

        tagMgr.createTagsFromAPICreateMessage(msg, vo.getUuid(), ZoneVO.class.getSimpleName());

        evt.setInventory(ZoneInventory.valueOf(vo));
        logger.debug("Created zone: " + vo.getName() + " uuid:" + vo.getUuid());
        bus.publish(evt);
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
