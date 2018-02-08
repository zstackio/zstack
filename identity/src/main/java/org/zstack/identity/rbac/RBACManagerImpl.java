package org.zstack.identity.rbac;

import org.zstack.header.Component;

public class RBACManagerImpl implements RBACManager, Component {

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
