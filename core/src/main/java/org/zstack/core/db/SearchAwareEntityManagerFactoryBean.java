package org.zstack.core.db;

import org.hibernate.search.exception.SearchException;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.zstack.core.search.SearchGlobalProperty;
import org.zstack.utils.ExceptionDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.EntityManagerFactory;
import java.sql.SQLException;

public class SearchAwareEntityManagerFactoryBean extends LocalContainerEntityManagerFactoryBean {
    private static final CLogger logger = Utils.getLogger(SearchAwareEntityManagerFactoryBean.class);
    private static final String REGISTER_SEARCH = "hibernate.search.autoregister_listeners";

    @Override
    protected EntityManagerFactory createNativeEntityManagerFactory() {
        try {
            return super.createNativeEntityManagerFactory();
        } catch (RuntimeException failure) {
            if (ExceptionDSL.isCausedBy(failure, InterruptedException.class)) {
                Thread.currentThread().interrupt();
                throw failure;
            }
            if (Thread.currentThread().isInterrupted()
                    || !ExceptionDSL.isCausedBy(failure, SearchException.class)
                    || ExceptionDSL.isCausedBy(failure, SQLException.class)
                    || ExceptionDSL.isCausedBy(failure, Error.class)
                    || "false".equalsIgnoreCase(String.valueOf(getJpaPropertyMap().get(REGISTER_SEARCH)))) {
                throw failure;
            }

            logger.warn("global search initialization failed; retry database initialization without search", failure);
            // A fresh factory omits the failed search listeners as well as the search integrator.
            getJpaPropertyMap().put(REGISTER_SEARCH, "false");
            EntityManagerFactory factory;
            try {
                factory = super.createNativeEntityManagerFactory();
            } catch (RuntimeException databaseFailure) {
                databaseFailure.addSuppressed(failure);
                throw databaseFailure;
            }
            SearchGlobalProperty.SearchAutoRegister = false;
            logger.warn("global search is unavailable; restore search cache communication and restart this management node to recover");
            return factory;
        }
    }
}
