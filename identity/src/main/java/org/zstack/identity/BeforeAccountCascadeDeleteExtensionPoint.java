package org.zstack.identity;

import org.zstack.core.cascade.CascadeAction;
import org.zstack.header.core.Completion;
import org.zstack.header.identity.AccountInventory;

public interface BeforeAccountCascadeDeleteExtensionPoint {
    void beforeDelete(AccountInventory account, CascadeAction action, Completion completion);

    void cancel(AccountInventory account, CascadeAction action, Completion completion);
}
