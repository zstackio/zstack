package org.zstack.identity.rbac;

import org.zstack.identity.rbac.datatype.RBACEntity;

public interface RBACEntityFormatter {
    Class[] getAPIClasses();

    RBACEntity format(RBACEntity entity);
}
