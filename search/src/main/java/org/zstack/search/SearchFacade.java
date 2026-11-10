package org.zstack.search;

import org.hibernate.search.jpa.FullTextEntityManager;

public interface SearchFacade {

    void checkSearchAvailability();

    FullTextEntityManager getFullTextEntityManager();
}
