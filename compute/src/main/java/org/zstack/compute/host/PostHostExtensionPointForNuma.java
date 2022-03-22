package org.zstack.compute.host;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PostHostExtensionPointForNuma implements PostHostConnectExtensionPoint {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    protected static final CLogger logger = Utils.getLogger(PostHostExtensionPointForNuma.class);

    @Override
    public Flow createPostHostConnectFlow(HostInventory host) {
        return new NoRollbackFlow() {
            String __name__ = "update-host-NUMA";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                GetHostNumaTopologyMsg msg = new GetHostNumaTopologyMsg();
                msg.setHostUuid(host.getUuid());
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply kreply) {
                        if (!kreply.isSuccess()) {
                            logger.error(String.format("Get Host[%s] NUMA Topology error: %s", host.getUuid(), kreply.getError().toString()));
                            trigger.next();
                        } else {
                            GetHostNumaTopologyReply rpy = (GetHostNumaTopologyReply) kreply;
                            Map<String, HostNUMANode> nodes = rpy.getNuma();
                            if (nodes == null || nodes.isEmpty()) {
                                trigger.next();
                                return;
                            }

                            SimpleQuery<HostNumaNodeVO> nodesQuery = dbf.createQuery(HostNumaNodeVO.class);
                            nodesQuery.add(HostNumaNodeVO_.hostUuid, SimpleQuery.Op.EQ, host.getUuid());
                            List<HostNumaNodeVO> numaNodes = nodesQuery.list();
                            if (!numaNodes.isEmpty()) {
                                dbf.removeCollection(numaNodes, HostNumaNodeVO.class);
                            }

                            Iterator<Map.Entry<String, HostNUMANode>> nodeEntries = nodes.entrySet().iterator();
                            while (nodeEntries.hasNext()) {
                                Map.Entry<String, HostNUMANode> node = nodeEntries.next();
                                HostNUMANode nodeInfo = node.getValue();

                                HostNumaNodeVO hntvo = new HostNumaNodeVO(nodeInfo);
                                hntvo.setHostUuid(host.getUuid());
                                hntvo.setNodeID(node.getKey());

                                dbf.persist(hntvo);
                            }
                            logger.info(String.format("Update Host[%s] NUMA Topology Successfully!", host.getUuid()));
                            trigger.next();
                        }
                    }
                });
            }
        };
    }
}
