package org.zstack.sdnController;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.VSwitchType;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class SdnControllerPrepareAttachTest {
    private Field loaderField;
    private Object previousLoader;
    private SdnControllerManagerImpl manager;
    private Map<String, SdnControllerFactory> factories;
    private static final String CONTROLLER_TYPE = "prepare-attach-test-controller";

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        loaderField = Platform.class.getDeclaredField("loader");
        loaderField.setAccessible(true);
        previousLoader = loaderField.get(null);
        ComponentLoader loader = mock(ComponentLoader.class);
        ErrorFacade errors = mock(ErrorFacade.class);
        when(loader.getComponent(ErrorFacade.class)).thenReturn(errors);
        when(errors.instantiateErrorCode(any(Enum.class), anyString(), isNull(ErrorCode.class))).thenAnswer(call -> {
            ErrorCode error = new ErrorCode();
            error.setCode("SYS.1007");
            error.setDescription("operation failed");
            error.setDetails(call.getArgument(1));
            return error;
        });
        loaderField.set(null, loader);
        manager = new SdnControllerManagerImpl();
        Field field = SdnControllerManagerImpl.class.getDeclaredField("sdnControllerFactories");
        field.setAccessible(true);
        factories = (Map<String, SdnControllerFactory>) field.get(manager);
    }

    @After
    public void tearDown() throws Exception {
        loaderField.set(null, previousLoader);
    }

    private L2NetworkInventory network(boolean requiresController) {
        VSwitchType type = new VSwitchType("prepare-attach-test-" + requiresController);
        type.setSdnControllerType(requiresController ? CONTROLLER_TYPE : null);
        L2NetworkInventory network = new L2NetworkInventory();
        network.setUuid("00000000000040008000000000000001");
        network.setvSwitchType(type.toString());
        return network;
    }

    @Test
    public void ordinaryNetworkNeedsNoController() {
        RecordingCompletion completion = new RecordingCompletion();
        manager.prepareAttach(network(false), "cluster", completion);
        assertTrue(completion.succeeded);
        assertNull(completion.error);
    }

    @Test
    public void missingFactoryRejectsAttach() {
        assertMissingControllerRejected();
    }

    @Test
    public void missingControllerMappingRejectsAttach() {
        factories.put(CONTROLLER_TYPE, mock(SdnControllerFactory.class));
        assertMissingControllerRejected();
    }

    private void assertMissingControllerRejected() {
        RecordingCompletion completion = new RecordingCompletion();
        manager.prepareAttach(network(true), "cluster", completion);
        assertFalse("SDN attach must fail closed without its controller", completion.succeeded);
        assertNotNull(completion.error);
        assertEquals("ORG_ZSTACK_SDNCONTROLLER_10043", completion.error.getGlobalErrorCode());
    }

    @Test
    public void mappedNetworkDelegatesCompletionToController() {
        L2NetworkInventory network = network(true);
        SdnControllerFactory factory = mock(SdnControllerFactory.class);
        SdnControllerL2 controller = mock(SdnControllerL2.class);
        when(factory.getSdnControllerL2(network.getUuid())).thenReturn(controller);
        factories.put(CONTROLLER_TYPE, factory);
        RecordingCompletion completion = new RecordingCompletion();
        manager.prepareAttach(network, "cluster", completion);
        verify(controller).prepareL2NetworkForCluster(network, "cluster", completion);
        assertFalse(completion.succeeded);
        assertNull(completion.error);
    }

    private static class RecordingCompletion extends Completion {
        private boolean succeeded;
        private ErrorCode error;
        RecordingCompletion() { super(null); }
        @Override public void success() { succeeded = true; }
        @Override public void fail(ErrorCode error) { this.error = error; }
    }
}
