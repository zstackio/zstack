package org.zstack.test.core.keyvalue;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.keyvalue.KeyValueFacade;
import org.zstack.core.keyvalue.KeyValueQuery;
import org.zstack.core.keyvalue.Op;
import org.zstack.test.BeanConstructor;
import org.zstack.test.DBUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 */
public class TestKeyValue1 {
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
        kvf.persist(e);

        e = new KeyValueTestEntity();
        e.list1.clear();
        e.list1.add(99);
        kvf.persist(e);

        e = new KeyValueTestEntity();
        e.list1.clear();
        e.list1.add(100);
        kvf.persist(e);

        e = new KeyValueTestEntity();
        e.dict = new HashMap<String, String>();
        kvf.persist(e);

        e = new KeyValueTestEntity();
        e.dict2 = new HashMap<String, KeyValueTestEntity>();
        e.dict2.put("xxx", new KeyValueTestEntity());
        kvf.persist(e);

        e = new KeyValueTestEntity();
        e.child = new KeyValueTestEntity();
        e.child.list1.clear();
        e.child.list1.add(11);
        e.child.list1.add(22);
        kvf.persist(e);

        e = new KeyValueTestEntity();
        e.d = null;
        kvf.persist(e);

        KeyValueQuery<KeyValueTestEntity> q = new KeyValueQuery<KeyValueTestEntity>(KeyValueTestEntity.class);
        q.and(q.entity().getDict2().get("xxx").getC2(), Op.EQ, "hello world");
        KeyValueTestEntity te = q.find();
        Assert.assertNotNull(te);
        Assert.assertEquals("hello world", te.c2);

        q = new KeyValueQuery<KeyValueTestEntity>(KeyValueTestEntity.class);
        q.and(q.entity().getList1().get(-1), Op.IN, 99, 100);
        List<KeyValueTestEntity> tes = q.list();
        Assert.assertEquals(2, tes.size());

        q = new KeyValueQuery<KeyValueTestEntity>(KeyValueTestEntity.class);
        q.and(q.entity().getList1().get(10), Op.NOT_IN, 99, 100);
        tes = q.list();
        Assert.assertEquals(0, tes.size());

        q = new KeyValueQuery<KeyValueTestEntity>(KeyValueTestEntity.class);
        q.and(q.entity().getChild().getList1().get(0), Op.EQ, 11);
        q.and(q.entity().getChild().getList1().get(1), Op.EQ, 22);
        te = q.find();
        Assert.assertNotNull(te);
        Assert.assertTrue(te.getChild().getList1().contains(11));
        Assert.assertTrue(te.getChild().getList1().contains(22));

        q = new KeyValueQuery<KeyValueTestEntity>(KeyValueTestEntity.class);
        q.and(q.entity().getChild().getA1(), Op.NOT_NULL);
        tes = q.list();
        Assert.assertEquals(1, tes.size());

        q = new KeyValueQuery<KeyValueTestEntity>(KeyValueTestEntity.class);
        q.and(q.entity().getC2(), Op.NULL);
        tes = q.list();
        Assert.assertEquals(0, tes.size());

        q = new KeyValueQuery<KeyValueTestEntity>(KeyValueTestEntity.class);
        q.and(q.entity().getD(), Op.NULL);
        q.and(q.entity().getA1(), Op.EQ, 10);
        tes = q.list();
        Assert.assertEquals(1, tes.size());
    }
}
