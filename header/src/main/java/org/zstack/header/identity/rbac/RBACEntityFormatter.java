package org.zstack.header.identity.rbac;

public interface RBACEntityFormatter {
    Class[] getAPIClasses();

    RBACEntity format(RBACEntity entity);
}
