package org.zstack.test.core.debug;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.debug.*;

import java.util.Collections;

/**
 * Created by david on 2/9/17.
 */
public class TestDebugManager {
    @InjectMocks
    private DebugManagerImpl debugManager;

    @Mock
    private CloudBus mockBus;

    @Before
    public void setUp() {
        debugManager = new DebugManagerImpl();

        // do this after initialize 'debugManager' so that
        // the 'mockBus' can be injected.
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testEmptyManager() {
        // The following setup is optional, since 'doNothing'
        // is the default behavior for stubbed void method.
        Mockito.doNothing()
                .when(mockBus)
                .publish(Mockito.any(APIDebugSignalEvent.class));

        APIDebugSignalMsg debugSignalMsg = new APIDebugSignalMsg();
        debugSignalMsg.setSignals(Collections.emptyList());
        debugManager.handleMessage(debugSignalMsg);
    }

    @Test
    public void testCustomHandler() {
        Assert.assertTrue(DebugManagerImpl.sigHandlers.isEmpty());

        String TEST_SIGNAL = "test";

        class MyDebugHandler implements DebugSignalHandler {
            private int count = 0;

            private int getCounter() {
                return count;
            }

            @Override
            public void handleDebugSignal() {
                ++count;
            }
        }

        MyDebugHandler handler = new MyDebugHandler();
        DebugManager.registerDebugSignalHandler(TEST_SIGNAL, handler);

        APIDebugSignalMsg debugSignalMsg = new APIDebugSignalMsg();
        debugSignalMsg.setSignals(Collections.singletonList(DebugSignal.DumpTaskQueue.toString()));
        debugManager.handleMessage(debugSignalMsg);
        Assert.assertTrue(handler.getCounter() == 1);
    }
}
