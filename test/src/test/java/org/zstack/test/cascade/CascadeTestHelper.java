package org.zstack.test.cascade;

import junit.framework.Assert;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CascadeTestHelper {
    @Autowired
    private DatabaseFacade dbf;

    public void zeroInDatabase(Class entityClass) {
        long count = dbf.count(entityClass);
        Assert.assertEquals(String.format("there are still records in DB for entity class[%s]", entityClass.getName()), 0, count);
    }

    public void zeroInDatabase(Class... entityClass) {
        for (Class ec : entityClass) {
            zeroInDatabase(ec);
        }
    }
}
