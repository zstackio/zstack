package org.zstack.plugin.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * Writes rows in one transaction: they are committed together, and rolled back together with the platform's rows when
 * the transaction fails. dbf.persist() would commit each row on its own (REQUIRES_NEW); use dbf.getEntityManager() or
 * SQLBatch inside a transaction. Replace with the plugin's own logic or remove it.
 */
public class ExampleTransactional {
    @Autowired
    private DatabaseFacade dbf;

    @Transactional
    public void persistAll(List<?> entities) {
        EntityManager em = dbf.getEntityManager();
        entities.forEach(em::persist);
    }
}
