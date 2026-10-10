package org.zstack.core.db;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs post-commit notifications in a fresh transaction when the completed
 * transaction's resources are still bound to the current thread. Without this
 * boundary, REQUIRED work started by an extension can join the already
 * committed transaction and never be committed itself.
 */
public final class AfterCommitTransactionExecutor {
    private AfterCommitTransactionExecutor() { }

    public static void run(PlatformTransactionManager manager, Runnable callback) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            callback.run();
            return;
        }
        if (manager == null) {
            throw new IllegalStateException("Cannot run post-commit extension work without a transaction manager");
        }

        TransactionTemplate template = new TransactionTemplate(manager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.execute(status -> {
            callback.run();
            return null;
        });
    }
}
