package org.zstack.core.config;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * Opt-in same-transaction participant shared by several configuration fields.
 * Invoked once per actual local mutation/batch, never during validation or
 * remote canonical replay. Both callbacks precede commit; external side effects
 * must be deferred to commit. Any callback failure aborts the whole transaction.
 */
public interface ConfigTransactionalMutationExtensionPoint {
    void beforeMutations(EntityManager em, List<ConfigMutation> changes, ConfigMutationContext context);
    void afterMutations(EntityManager em, List<ConfigMutation> changes, ConfigMutationContext context);
}
