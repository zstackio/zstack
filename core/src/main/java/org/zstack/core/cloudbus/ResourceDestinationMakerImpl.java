package org.zstack.core.cloudbus;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
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
    private ConsistentHash<String> nodeHash = new ConsistentHash<>(new ApacheHash(), 500, new ArrayList<String>()) ;
    private Map<String, NodeInfo> nodes = new HashMap<>();

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void nodeJoin(String nodeId) {
        ManagementNodeVO vo = dbf.findByUuid(nodeId, ManagementNodeVO.class);
        nodeHash.add(nodeId);
        nodes.put(vo.getUuid(), new NodeInfo(vo));
    }

    @Override
    public void nodeLeft(String nodeId) {
        nodeHash.remove(nodeId);
        nodes.remove(nodeId);
    }

    @Override
    public void iAmDead(String nodeId) {
        nodeHash.remove(nodeId);
        nodes.remove(nodeId);
    }

    @Override
    public void iJoin(String nodeId) {
        List<ManagementNodeVO> lst = Q.New(ManagementNodeVO.class).list();
        lst.forEach((ManagementNodeVO node) -> {
            nodeHash.add(node.getUuid());
            nodes.put(node.getUuid(), new NodeInfo(node));
        });
    }

    @Override
    public String makeDestination(String resourceUuid) {
        String nodeUuid = nodeHash.get(resourceUuid);
        if (nodeUuid == null) {
            throw new CloudRuntimeException("Cannot find any available management node to send message");
        }

        return nodeUuid;
    }

    @Override
    public boolean isManagedByUs(String resourceUuid) {
        String nodeUuid = makeDestination(resourceUuid);
        return nodeUuid.equals(Platform.getManagementServerId());
    }

    @Override
    public Collection<String> getManagementNodesInHashRing() {
        return nodeHash.getNodes();
    }

    @Override
    public NodeInfo getNodeInfo(String nodeUuid) {
        NodeInfo info = nodes.get(nodeUuid);
        if (info == null) {
            ManagementNodeVO vo = dbf.findByUuid(nodeUuid, ManagementNodeVO.class);
            if (vo == null) {
                throw new CloudRuntimeException(String.format("cannot find management node[uuid:%s]", nodeUuid));
            }

            nodeHash.add(nodeUuid);
            info = nodes.put(nodeUuid, new NodeInfo(vo));
        }

        return info;
    }

    @Override
    public Collection<NodeInfo> getAllNodeInfo() {
        return nodes.values();
    }

    @Override
    public int getManagementNodeCount() {
        return nodes.values().size();
    }


    public boolean isNodeInCircle(String nodeId) {
        return nodeHash.hasNode(nodeId);
    }
}
