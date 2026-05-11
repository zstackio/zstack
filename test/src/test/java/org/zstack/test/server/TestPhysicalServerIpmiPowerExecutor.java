package org.zstack.test.server;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.server.PhysicalServerVO;
import org.zstack.server.PhysicalServerIpmiPowerExecutor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

public class TestPhysicalServerIpmiPowerExecutor {
    private final PhysicalServerIpmiPowerExecutor executor = new PhysicalServerIpmiPowerExecutor();

    @Before
    public void setUp() {
        CoreGlobalProperty.UNIT_TEST_ON = true;
    }

    @After
    public void tearDown() {
        CoreGlobalProperty.UNIT_TEST_ON = false;
    }

    @Test
    public void powerOnPxeSucceedsInUnitTestMode() {
        PhysicalServerVO server = serverWithOob();
        Result result = new Result();

        executor.powerOnPxe(server, result.completion());

        Assert.assertNull("powerOnPxe should succeed in unit-test mode", result.error);
        Assert.assertTrue("powerOnPxe should invoke success callback", result.succeeded);
    }

    @Test
    public void powerOnPxeFailsWhenOobCredentialsMissing() {
        PhysicalServerVO server = new PhysicalServerVO();
        server.setUuid("server-no-oob");
        Result result = new Result();
        ErrorCode stubError = new ErrorCode("OPERATION.ERROR", "operation error",
                "OOB credentials not configured for PhysicalServer[uuid:server-no-oob]");

        try (MockedStatic<Platform> platform = mockStatic(Platform.class)) {
            platform.when(() -> Platform.operr(anyString(), any()))
                    .thenReturn(stubError);
            platform.when(() -> Platform.operr(anyString(), any(), any()))
                    .thenReturn(stubError);

            executor.powerOnPxe(server, result.completion());
        }

        Assert.assertFalse("powerOnPxe should not succeed without OOB credentials", result.succeeded);
        Assert.assertNotNull("powerOnPxe should report error for missing OOB credentials", result.error);
        Assert.assertTrue("error message should mention OOB credentials",
                result.error.getDetails().contains("OOB credentials not configured"));
    }

    private static PhysicalServerVO serverWithOob() {
        PhysicalServerVO server = new PhysicalServerVO();
        server.setUuid("server-with-oob");
        server.setOobManagementType("IPMI");
        server.setOobAddress("192.168.0.20");
        server.setOobPort(623);
        server.setOobUsername("admin");
        server.setOobPassword("password");
        return server;
    }

    private static class Result {
        boolean succeeded;
        ErrorCode error;

        Completion completion() {
            return new Completion(null) {
                @Override
                public void success() {
                    succeeded = true;
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    error = errorCode;
                }
            };
        }
    }
}
