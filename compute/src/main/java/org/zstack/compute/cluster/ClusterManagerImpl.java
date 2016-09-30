package org.zstack.compute.cluster;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.DbEntityLister;
import org.zstack.core.defer.Deferred;
import org.zstack.header.AbstractService;
import org.zstack.header.cluster.*;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.search.GetQuery;
import org.zstack.search.SearchQuery;
import org.zstack.tag.TagManager;
import org.zstack.utils.ObjectUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.FieldPrinter;
import org.zstack.utils.logging.CLogger;

import java.util.*;

public class ClusterManagerImpl extends AbstractService implements ClusterManager {
	private static final CLogger logger = Utils.getLogger(ClusterManager.class);
	private static final FieldPrinter printer = Utils.getFieldPrinter();

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


	private Map<String, ClusterFactory> clusterFactories = Collections.synchronizedMap(new HashMap<String, ClusterFactory>());
    private static final Set<Class> allowedMessageAfterSoftDeletion = new HashSet<Class>();

    static {
        allowedMessageAfterSoftDeletion.add(ClusterDeletionMsg.class);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateClusterMsg) {
            handle((APICreateClusterMsg) msg);
        } else if (msg instanceof APIListClusterMsg) {
            handle((APIListClusterMsg) msg);
        } else if (msg instanceof APISearchClusterMsg) {
            handle((APISearchClusterMsg) msg);
        } else if (msg instanceof APIGetClusterMsg) {
            handle((APIGetClusterMsg) msg);
        } else if (msg instanceof ClusterMessage) {
            passThrough((ClusterMessage) msg);
        }
    }

    private void handle(APIGetClusterMsg msg) {
	    GetQuery q = new GetQuery();
	    String res = q.getAsString(msg, ClusterInventory.class);
	    APIGetClusterReply reply = new APIGetClusterReply();
	    reply.setInventory(res);
	    bus.reply(msg, reply);
    }

    private void handle(APISearchClusterMsg msg) {
	    SearchQuery<ClusterInventory> query = SearchQuery.create(msg, ClusterInventory.class);
	    String ret = query.listAsString();
	    APISearchClusterReply reply = new APISearchClusterReply();
	    reply.setContent(ret);
	    bus.reply(msg, reply);
    }

    private void passThrough(ClusterMessage msg) {
        ClusterVO vo = dbf.findByUuid(msg.getClusterUuid(), ClusterVO.class);
        if (vo == null && allowedMessageAfterSoftDeletion.contains(msg.getClass())) {
            ClusterEO eo = dbf.findByUuid(msg.getClusterUuid(), ClusterEO.class);
            vo = ObjectUtils.newAndCopy(eo, ClusterVO.class);
        }

		if (vo == null) {
			String err = String.format("Cannot find cluster: %s, it may have been deleted", msg.getClusterUuid());
			bus.replyErrorByMessageType((Message) msg, err);
			return;
		}
		
		ClusterFactory factory = this.getClusterFactory(ClusterType.valueOf(vo.getType()));
		Cluster cluster = factory.getCluster(vo);
		cluster.handleMessage((Message) msg);
	}


	private void handle(APIListClusterMsg msg) {
		APIListClusterReply reply = new APIListClusterReply();

		List<ClusterVO> vos = dl.listByApiMessage(msg, ClusterVO.class);
		List<ClusterInventory> invs = ClusterInventory.valueOf(vos);
		reply.setInventories(invs);
		bus.reply(msg, reply);
	}

	@Deferred
	private void handle(APICreateClusterMsg msg) {
	    String clusterType = msg.getType();
	    if (clusterType == null) {
	        clusterType = BaseClusterFactory.type.toString();
	    }
	    
		APICreateClusterEvent evt = new APICreateClusterEvent(msg.getId());
	    ClusterFactory factory = this.getClusterFactory(ClusterType.valueOf(clusterType));
        ClusterVO vo = new ClusterVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setDescription(msg.getDescription());
        vo.setHypervisorType(msg.getHypervisorType());
        vo.setManagementNodeId(Platform.getManagementServerId());
        vo.setZoneUuid(msg.getZoneUuid());
        vo.setState(ClusterState.Enabled);
        vo.setName(msg.getClusterName());
	    vo = factory.createCluster(vo, msg);

        tagMgr.createTagsFromAPICreateMessage(msg, vo.getUuid(), ClusterVO.class.getSimpleName());

	    ClusterInventory inv = ClusterInventory.valueOf(vo);
	    evt.setInventory(inv);
	    logger.debug(String.format("Created new cluster: %s", printer.print(inv)));
	    bus.publish(evt);
	}

	@Override
    @MessageSafe
	public void handleMessage(Message msg) {
		if (msg instanceof APIMessage) {
			handleApiMessage((APIMessage) msg);
		} else if (msg instanceof ClusterMessage) {
			passThrough((ClusterMessage) msg);
		} else {
			bus.dealWithUnknownMessage(msg);
		}
	}

	@Override
	public String getId() {
		return bus.makeLocalServiceId(ClusterConstant.SERVICE_ID);
	}

    private void populateClusterFactories() {
        for (ClusterFactory ext : pluginRgty.getExtensionList(ClusterFactory.class)) {
            ClusterFactory old = clusterFactories.get(ext.getType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate ClusterFactory[%s, %s] for type[%s]",
                        old.getClass().getName(), ext.getClass().getName(), ext.getType()));
            }
            clusterFactories.put(ext.getType().toString(), ext);
        }
    }
	   
	@Override
	public boolean start() {
	    populateClusterFactories();
		return true;
	}

	@Override
	public boolean stop() {
		return true;
	}

    private ClusterFactory getClusterFactory(ClusterType type) {
        ClusterFactory factory = clusterFactories.get(type.toString());
        if (factory == null) {
            throw new CloudRuntimeException(String.format("No ClusterFactory of type[%s] found", type));
        }
        return factory;
    }
}
