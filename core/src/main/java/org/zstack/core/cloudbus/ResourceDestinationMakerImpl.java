package org.zstack.core.cloudbus;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.utils.hash.ApacheHash;
import org.zstack.utils.hash.ConsistentHash;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 12:36 AM
 * To change this template use File | Settings | File Templates.
 */
public class ResourceDestinationMakerImpl implements ManagementNodeChangeListener, ResourceDestinationMaker {
    private final ConsistentHash<String> nodeHash = new ConsistentHash<>(new ApacheHash(), 500, new ArrayList<String>()) ;
    private final Map<String, NodeInfo> nodes = new HashMap<>();

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public synchronized void nodeJoin(ManagementNodeInventory inv) {
        nodeHash.add(inv.getUuid());
        nodes.put(inv.getUuid(), new NodeInfo(inv));
    }

    @Override
    public synchronized void nodeLeft(ManagementNodeInventory inv) {
        String nodeId = inv.getUuid();
        nodeHash.remove(nodeId);
        nodes.remove(nodeId);
    }

    @Override
    public synchronized void iAmDead(ManagementNodeInventory inv) {
        String nodeId = inv.getUuid();
        nodeHash.remove(nodeId);
        nodes.remove(nodeId);
    }

    @Override
    public synchronized void iJoin(ManagementNodeInventory inv) {
        List<ManagementNodeVO> lst = Q.New(ManagementNodeVO.class).list();
        lst.forEach((ManagementNodeVO node) -> {
            nodeHash.add(node.getUuid());
            nodes.put(node.getUuid(), new NodeInfo(node));
        });
    }

    @Override
    public synchronized String makeDestination(String resourceUuid) {
        String nodeUuid = nodeHash.get(resourceUuid);
        if (nodeUuid == null) {
            throw new CloudRuntimeException("Cannot find any available management node to send message");
        }

        return nodeUuid;
    }

    @Override
    public synchronized boolean isManagedByUs(String resourceUuid) {
        String nodeUuid = makeDestination(resourceUuid);
        return nodeUuid.equals(Platform.getManagementServerId());
    }

    @Override
    public synchronized Collection<String> getManagementNodesInHashRing() {
        return new ArrayList<>(nodeHash.getNodes());
    }

    @Override
    public synchronized NodeInfo getNodeInfo(String nodeUuid) {
        NodeInfo info = nodes.get(nodeUuid);
        if (info == null) {
            ManagementNodeVO vo = dbf.findByUuid(nodeUuid, ManagementNodeVO.class);
            if (vo == null) {
                throw new ManagementNodeNotFoundException(nodeUuid);
            }

            nodeHash.add(nodeUuid);
            info = nodes.put(nodeUuid, new NodeInfo(vo));
        }

        return info;
    }

    @Override
    public synchronized Collection<NodeInfo> getAllNodeInfo() {
        return new ArrayList<>(nodes.values());
    }

    @Override
    public synchronized int getManagementNodeCount() {
        return nodes.size();
    }


    public synchronized boolean isNodeInCircle(String nodeId) {
        return nodeHash.hasNode(nodeId);
    }
}
