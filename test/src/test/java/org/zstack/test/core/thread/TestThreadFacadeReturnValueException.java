package org.zstack.test.core.thread;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.thread.Task;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


public class TestThreadFacadeReturnValueException {
    private static final CLogger logger = Utils.getLogger(TestThreadFacadeReturnValueException.class);
    ComponentLoader loader;
    ThreadFacade thdf;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    class Tester implements Task<Integer> {
        int index;

        Tester(int index) {
            this.index = index;
        }

        @Override
        public Integer call() throws Exception {
            throw new CloudRuntimeException("This is on purpose");
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
        thrown.expect(ExecutionException.class);
        Tester worker = new Tester(100);
        Future<Integer> f = thdf.submit(worker);
        Integer ret = f.get();
    }
}
