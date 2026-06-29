package org.zstack.core.search;

import org.hibernate.search.backend.jgroups.impl.NodeSelectorStrategy;
import org.jgroups.Address;
import org.jgroups.View;
import org.zstack.core.Platform;

public class ZStackZSha2NodeSelector implements NodeSelectorStrategy {
    @Override
    public boolean isIndexOwnerLocal() {
        return Platform.isVIPNode();
    }

    @Override
    public void setLocalAddress(Address address) {
    }

    @Override
    public void viewAccepted(View view) {
    }
}
