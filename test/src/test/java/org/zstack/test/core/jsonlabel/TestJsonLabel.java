package org.zstack.test.core.jsonlabel;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.jsonlabel.JsonLabel;
import org.zstack.core.jsonlabel.JsonLabelInventory;
import org.zstack.core.thread.AsyncThread;
import org.zstack.test.BeanConstructor;
import org.zstack.test.DBUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;

public class TestJsonLabel {
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        dbf = loader.getComponent(DatabaseFacade.class);
        DBUtil.reDeployDB();
    }

    public static class Item {
        String word;
    }

    @Test
    public void test() throws InterruptedException {
        JsonLabelInventory inv = new JsonLabel().create("key", "value");
        Assert.assertEquals("key", inv.getLabelKey());
        Assert.assertEquals("value", inv.getLabelValue());
        Assert.assertEquals("value", new JsonLabel().get("key", String.class));
        Assert.assertTrue(new JsonLabel().exists("key"));
        Assert.assertTrue(new JsonLabel().atomicExists("key"));

        new JsonLabel().delete("key");
        Assert.assertFalse(new JsonLabel().exists("key"));
        Assert.assertFalse(new JsonLabel().atomicExists("key"));
        Assert.assertEquals(null, new JsonLabel().get("key", String.class));

        int num = 3;
        CountDownLatch latch = new CountDownLatch(num);
        for (int i = 0; i < num; i++) {
            new Runnable() {
                @Override
                @AsyncThread
                public void run() {
                    try {
                        new JsonLabel().createIfAbsent("k2", "v2");
                    } finally {
                        latch.countDown();
                    }
                }
            }.run();
        }

        latch.await(1, TimeUnit.MINUTES);
        Assert.assertEquals("v2", new JsonLabel().get("k2", String.class));

        CountDownLatch latch2 = new CountDownLatch(num);
        for (int i = 0; i < num; i++) {
            new Runnable() {
                @Override
                @AsyncThread
                public void run() {
                    try {
                        new JsonLabel().atomicDelete("k2");
                    } finally {
                        latch2.countDown();
                    }
                }
            }.run();
        }

        latch2.await(1, TimeUnit.MINUTES);
        Assert.assertFalse(new JsonLabel().exists("k2"));


        List<Integer> lst = asList(1, 2, 3);
        new JsonLabel().create("k3", lst);
        List<Integer> lst2 = (List<Integer>) new JsonLabel().getAsCollection("k3", ArrayList.class, Integer.class);
        Assert.assertEquals(3, lst2.size());

        Item item = new Item();
        item.word = "hello";
        inv = new JsonLabel().create("k4", item, "abcd");
        Assert.assertEquals("abcd", inv.getResourceUuid());
        item = new JsonLabel().get("k4", Item.class);
        Assert.assertEquals("hello", item.word);
    }
}
