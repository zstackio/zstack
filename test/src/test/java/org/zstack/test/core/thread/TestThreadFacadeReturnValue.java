package org.zstack.test.core.thread;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.thread.Task;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


public class TestThreadFacadeReturnValue {
    private static final CLogger logger = Utils.getLogger(TestThreadFacadeReturnValue.class);
    ComponentLoader loader;
    ThreadFacade thdf;

    class Tester implements Task<Integer> {
        int index;

        Tester(int index) {
            this.index = index;
        }

        @Override
        public Integer call() throws Exception {
            return index;
        }

        @Override
        public String getName() {
            return "Tester";
        }
    }

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        thdf = loader.getComponent(ThreadFacade.class);
    }

    @Test
    public void test() throws InterruptedException, ExecutionException {
        Tester worker = new Tester(100);
        Future<Integer> f = thdf.submit(worker);
        Integer ret = f.get();
        logger.info("Return: " + ret);
        Assert.assertEquals(100, ret.intValue());
    }

}
