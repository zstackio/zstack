package org.zstack.test.core.keyvalue;

import junit.framework.Assert;
import org.apache.commons.lang.time.StopWatch;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.keyvalue.KeyValueFacade;
import org.zstack.core.keyvalue.KeyValueQuery;
import org.zstack.core.keyvalue.Op;
import org.zstack.test.BeanConstructor;
import org.zstack.test.DBUtil;

import java.util.ArrayList;

/**
 */
public class TestKeyValue2 {
    ComponentLoader loader;
    KeyValueFacade kvf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        kvf = loader.getComponent(KeyValueFacade.class);
    }

    @Test
    public void test() {
        KeyValueTestEntity e = new KeyValueTestEntity();
        e.list2 = new ArrayList<KeyValueTestEntity>();
        e.list2.add(new KeyValueTestEntity());
        e.list2.add(new KeyValueTestEntity());
        for (int i = 0; i < 1000; i++) {
            e.list2.add(new KeyValueTestEntity());
        }
        StopWatch w = new StopWatch();
        w.start();
        kvf.persist(e);
        w.stop();
        System.out.println(String.format("cost %s", w.getTime()));

        w = new StopWatch();
        w.start();
        KeyValueQuery<KeyValueTestEntity> q = new KeyValueQuery<KeyValueTestEntity>(KeyValueTestEntity.class);
        q.and(q.entity().getList2().get(1).getA1(), Op.EQ, 10);
        e = q.find();
        w.stop();
        System.out.println(String.format("find cost %s", w.getTime()));
        Assert.assertNotNull(e);
    }
}
