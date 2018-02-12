package org.zstack.core.cloudbus;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.managementnode.ManagementNodeVO_;
import org.zstack.utils.hash.ApacheHash;
import org.zstack.utils.hash.ConsistentHash;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 12:36 AM
 * To change this template use File | Settings | File Templates.
 */
public class ResourceDestinationMakerImpl implements ManagementNodeChangeListener, ResourceDestinationMaker {
    private ConsistentHash<String> nodeHash = new ConsistentHash<String>(new ApacheHash(), 500, new ArrayList<String>()) ;

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void nodeJoin(String nodeId) {
        nodeHash.add(nodeId);
    }

    @Override
    public void nodeLeft(String nodeId) {
        nodeHash.remove(nodeId);
    }

    @Override
    public void iAmDead(String nodeId) {
        nodeHash.remove(nodeId);
    }

    @Override
    public void iJoin(String nodeId) {
        SimpleQuery<ManagementNodeVO> q = dbf.createQuery(ManagementNodeVO.class);
        q.select(ManagementNodeVO_.uuid);
        List<String> nodeIds = q.listValue();
        for (String id : nodeIds) {
            nodeHash.add(id);
        }
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

    public boolean isNodeInCircle(String nodeId) {
        return nodeHash.hasNode(nodeId);
    }
}
