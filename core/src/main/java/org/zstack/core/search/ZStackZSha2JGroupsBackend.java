package org.zstack.core.search;

import org.hibernate.search.backend.jgroups.impl.JGroupsBackend;
import org.hibernate.search.backend.jgroups.impl.NodeSelectorStrategy;
import org.hibernate.search.indexes.spi.IndexManager;

public class ZStackZSha2JGroupsBackend extends JGroupsBackend {
    @Override
    protected NodeSelectorStrategy createNodeSelectorStrategy(IndexManager indexManager) {
        return new ZStackZSha2NodeSelector();
    }
}
