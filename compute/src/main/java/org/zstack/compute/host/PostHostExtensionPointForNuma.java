package org.zstack.compute.host;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
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
                                SQL.New(HostNumaNodeVO.class).eq(HostNumaNodeVO_.hostUuid, host.getUuid()).hardDelete();
                                trigger.next();
                                return;
                            }

                            List<HostNumaNodeVO> oldHostNumaNodes = Q.New(HostNumaNodeVO.class)
                                    .eq(HostNumaNodeVO_.hostUuid, host.getUuid())
                                    .list();
                            
                            List<HostNumaNodeVO> newHostNumaNodes = new ArrayList<>();
                            Iterator<Map.Entry<String, HostNUMANode>> nodeEntries = nodes.entrySet().iterator();
                            while (nodeEntries.hasNext()) {
                                Map.Entry<String, HostNUMANode> node = nodeEntries.next();
                                HostNUMANode nodeInfo = node.getValue();
                                HostNumaNodeVO hntvo = new HostNumaNodeVO(nodeInfo);
                                hntvo.setHostUuid(host.getUuid());
                                hntvo.setNodeID(node.getKey());
                                newHostNumaNodes.add(hntvo);
                            }

                            oldHostNumaNodes.stream()
                                    .filter(oldNumaNode -> newHostNumaNodes.stream()
                                            .noneMatch(newNumaNode -> newNumaNode.getNodeCPUs().equals(oldNumaNode.getNodeCPUs())
                                                    && newNumaNode.getNodeDistance().equals(oldNumaNode.getNodeDistance())
                                                    && newNumaNode.getNodeMemSize() == oldNumaNode.getNodeMemSize()
                                                    && newNumaNode.getNodeID() == oldNumaNode.getNodeID()))
                                    .forEach(oldNumaNode -> dbf.removeByPrimaryKey(oldNumaNode.getId(), HostNumaNodeVO.class));

                            newHostNumaNodes.stream()
                                    .filter(newNumaNode -> oldHostNumaNodes.stream()
                                            .noneMatch(oldNumaNode -> oldNumaNode.getNodeCPUs().equals(newNumaNode.getNodeCPUs())
                                                    && oldNumaNode.getNodeDistance().equals(newNumaNode.getNodeDistance())
                                                    && oldNumaNode.getNodeMemSize() == newNumaNode.getNodeMemSize()
                                                    && oldNumaNode.getNodeID() == newNumaNode.getNodeID()))
                                    .forEach(newNumaNode -> dbf.persist(newNumaNode));
                            
                            logger.info(String.format("Update Host[%s] NUMA Topology Successfully!", host.getUuid()));
                            trigger.next();
                        }
                    }
                });
            }
        };
    }
}
