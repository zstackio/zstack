package org.zstack.network.l2;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.DbEntityLister;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.AbstractService;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.network.*;
import org.zstack.header.network.l2.*;
import org.zstack.search.GetQuery;
import org.zstack.search.SearchQuery;
import org.zstack.tag.TagManager;
import org.zstack.utils.ObjectUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

public class L2NetworkManagerImpl extends AbstractService implements L2NetworkManager {
    private static final CLogger logger = Utils.getLogger(L2NetworkManagerImpl.class);
    
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private DbEntityLister dl;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private ErrorFacade errf;

    private Map<String, L2NetworkFactory> l2NetworkFactories = Collections.synchronizedMap(new HashMap<String, L2NetworkFactory>());
    private Map<L2NetworkType, Map<HypervisorType, L2NetworkRealizationExtensionPoint>> realizationExts = new HashMap<L2NetworkType, Map<HypervisorType, L2NetworkRealizationExtensionPoint>>();
    private List<L2NetworkCreateExtensionPoint> createExtensions = new ArrayList<L2NetworkCreateExtensionPoint>();
    private static final Set<Class> allowedMessageAfterSoftDeletion = new HashSet<Class>();

    static {
        allowedMessageAfterSoftDeletion.add(L2NetworkDeletionMsg.class);
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
        if (msg instanceof L2NetworkMessage) {
            passThrough((L2NetworkMessage)msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateL2NetworkMsg) {
            handle((APICreateL2NetworkMsg)msg);
        } else if (msg instanceof APIListL2NetworkMsg) {
            handle((APIListL2NetworkMsg) msg);
        } else if (msg instanceof APISearchL2NetworkMsg) {
        	handle((APISearchL2NetworkMsg)msg);
        } else if (msg instanceof APISearchL2VlanNetworkMsg) {
            handle((APISearchL2VlanNetworkMsg)msg);
        } else if (msg instanceof APIGetL2NetworkMsg) {
            handle((APIGetL2NetworkMsg) msg);
        } else if (msg instanceof APIGetL2VlanNetworkMsg) {
            handle((APIGetL2VlanNetworkMsg)msg);
        } else if (msg instanceof APIGetL2NetworkTypesMsg) {
            handle((APIGetL2NetworkTypesMsg) msg);
        } else if (msg instanceof L2NetworkMessage) {
            passThrough((L2NetworkMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetL2NetworkTypesMsg msg) {
        List<String> types = new ArrayList<String>();
        types.addAll(L2NetworkType.getAllTypeNames());
        APIGetL2NetworkTypesReply reply = new APIGetL2NetworkTypesReply();
        reply.setL2NetworkTypes(types);
        bus.reply(msg, reply);
    }

    private void handle(APIGetL2VlanNetworkMsg msg) {
        GetQuery q = new GetQuery();
        String res = q.getAsString(msg, L2VlanNetworkInventory.class);
        APIGetL2VlanNetworkReply reply = new APIGetL2VlanNetworkReply();
        reply.setInventory(res);
        bus.reply(msg, reply);
    }

    private void handle(APISearchL2VlanNetworkMsg msg) {
        SearchQuery<L2VlanNetworkInventory> sq = SearchQuery.create(msg, L2VlanNetworkInventory.class);
        String content = sq.listAsString();
        APISearchL2VlanNetworkReply reply = new APISearchL2VlanNetworkReply();
        reply.setContent(content);
        bus.reply(msg, reply);
        
    }

    private void handle(APIGetL2NetworkMsg msg) {
        GetQuery q = new GetQuery();
        String res = q.getAsString(msg, L2NetworkInventory.class);
        APIGetL2NetworkReply reply = new APIGetL2NetworkReply();
        reply.setInventory(res);
        bus.reply(msg, reply);
    }

    private void handle(APISearchL2NetworkMsg msg) {
    	SearchQuery<L2NetworkInventory> sq = SearchQuery.create(msg, L2NetworkInventory.class);
    	String content = sq.listAsString();
    	APISearchL2NetworkReply reply = new APISearchL2NetworkReply();
    	reply.setContent(content);
    	bus.reply(msg, reply);
	}

	private void passThrough(L2NetworkMessage msg) {
        Message amsg = (Message) msg;
        L2NetworkVO vo = dbf.findByUuid(msg.getL2NetworkUuid(), L2NetworkVO.class);
        if (vo == null && allowedMessageAfterSoftDeletion.contains(msg.getClass())) {
            L2NetworkEO eo = dbf.findByUuid(msg.getL2NetworkUuid(), L2NetworkEO.class);
            vo = ObjectUtils.newAndCopy(eo, L2NetworkVO.class);
        }

        if (vo == null) {
            ErrorCode errCode = errf.instantiateErrorCode(SysErrors.RESOURCE_NOT_FOUND, String.format("unable to find L2Network[uuid:%s], it may have been deleted", msg.getL2NetworkUuid()));
            bus.replyErrorByMessageType((Message)msg, errCode);
            return;
        }
        
        L2NetworkFactory factory = getL2NetworkFactory(L2NetworkType.valueOf(vo.getType()));
        L2Network nw = factory.getL2Network(vo);
        nw.handleMessage(amsg);
    }
    
    private void handle(APIListL2NetworkMsg msg) {
        List<L2NetworkVO> vos = dl.listByApiMessage(msg, L2NetworkVO.class);
        List<L2NetworkInventory> invs = L2NetworkInventory.valueOf(vos);
        APIListL2NetworkReply reply = new APIListL2NetworkReply();
        reply.setInventories(invs);
        bus.reply(msg, reply);
    }

    private void handle(APICreateL2NetworkMsg msg) {
    	for (L2NetworkCreateExtensionPoint extp : createExtensions) {
    		try {
				extp.beforeCreateL2Network(msg);
			} catch (NetworkException e) {
				String err = String.format("unable to create l2network[name:%s, type:%s], %s", msg.getName(), msg.getType(), e.getMessage());
				logger.warn(err, e);
				APICreateL2NetworkEvent evt = new APICreateL2NetworkEvent(msg.getId());
                evt.setError(errf.instantiateErrorCode(SysErrors.CREATE_RESOURCE_ERROR, err));
				bus.publish(evt);
				return;
			}
    	}

        L2NetworkType type = L2NetworkType.valueOf(msg.getType());
        L2NetworkFactory factory = getL2NetworkFactory(type);
        L2NetworkVO vo = new L2NetworkVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setDescription(msg.getDescription());
        vo.setName(msg.getName());
        vo.setPhysicalInterface(msg.getPhysicalInterface());
        vo.setType(type.toString());
        vo.setZoneUuid(msg.getZoneUuid());
        L2NetworkInventory inv = factory.createL2Network(vo, msg);

        tagMgr.createTagsFromAPICreateMessage(msg, inv.getUuid(), L2NetworkVO.class.getSimpleName());

    	for (L2NetworkCreateExtensionPoint extp : createExtensions) {
    		try {
    			extp.afterCreateL2Network(inv);
    		} catch (Exception e) {
    			logger.warn(String.format("unhandled exception happened when calling %s", extp.getClass().getName()), e);
    		}
    	}

        APICreateL2NetworkEvent evt = new APICreateL2NetworkEvent(msg.getId());
        evt.setInventory(inv);
        bus.publish(evt);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(L2NetworkConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    @Override
    public boolean stop() {
        return false;
    }

    @Override
    public L2NetworkFactory getL2NetworkFactory(L2NetworkType type) {
        L2NetworkFactory factory = l2NetworkFactories.get(type.toString());
        if (factory == null) {
            throw new CloudRuntimeException(String.format("Cannot find L2NetworkFactory for type(%s)", type));
        }
        
        return factory;
    }
    
    @Override
    public L2NetworkRealizationExtensionPoint getRealizationExtension(L2NetworkType l2Type, HypervisorType hvType) {
        Map<HypervisorType, L2NetworkRealizationExtensionPoint> map = realizationExts.get(l2Type);
        if (map == null) {
            throw new IllegalArgumentException(String.format("Cannot find L2NetworkRealizationExtensionPoint supporting L2NetworkType[%s]", l2Type));
        }
        
        L2NetworkRealizationExtensionPoint extp = map.get(hvType);
        if (extp == null) {
            throw new IllegalArgumentException(String.format("Cannot find L2NetworkRealizationExtensionPoint for L2NetworkType[%s] supporting hypervisor[%s]", l2Type, hvType));
        }
        
        return extp;
    }
    
    private void populateExtensions() {
        for (L2NetworkFactory f : pluginRgty.getExtensionList(L2NetworkFactory.class)) {
            L2NetworkFactory old = l2NetworkFactories.get(f.getType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate L2NetworkFactory[%s, %s] for type[%s]",
                        f.getClass().getName(), old.getClass().getName(), f.getType()));
            }
            l2NetworkFactories.put(f.getType().toString(), f);
        }
        
        for (L2NetworkRealizationExtensionPoint extp : pluginRgty.getExtensionList(L2NetworkRealizationExtensionPoint.class)) {
            Map<HypervisorType, L2NetworkRealizationExtensionPoint> map = realizationExts.get(extp.getSupportedL2NetworkType());
            if (map == null) {
                map = new HashMap<HypervisorType, L2NetworkRealizationExtensionPoint>(1);
                realizationExts.put(extp.getSupportedL2NetworkType(), map);
            }
            map.put(extp.getSupportedHypervisorType(), extp);
        }

        createExtensions = pluginRgty.getExtensionList(L2NetworkCreateExtensionPoint.class);
    }
}
