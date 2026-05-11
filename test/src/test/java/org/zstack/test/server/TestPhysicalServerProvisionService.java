package org.zstack.test.server;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.server.APIProvisionPhysicalServerMsg;
import org.zstack.header.server.PhysicalServerProvisionNetworkInventory;
import org.zstack.header.server.PhysicalServerProvisionNetworkPoolRefVO;
import org.zstack.header.server.PhysicalServerProvisionNetworkVO;
import org.zstack.header.server.PhysicalServerHardwareDetailVO;
import org.zstack.header.server.PhysicalServerProvisionTarget;
import org.zstack.header.server.PhysicalServerVO;
import org.zstack.header.server.ProvisionNetworkState;
import org.zstack.header.server.ProvisionNetworkType;
import org.zstack.header.server.ProvisionProvider;
import org.zstack.header.server.ProvisionRequest;
import org.zstack.header.server.ProvisionResult;
import org.zstack.server.PhysicalServerProvisionService;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class TestPhysicalServerProvisionService {
    private static final String SERVER_UUID = "server-uuid";
    private static final String NETWORK_UUID = "network-uuid";
    private static final String POOL_UUID = "pool-uuid";
    private static final String ZONE_UUID = "zone-uuid";
    private static final String ACCOUNT_UUID = "account-uuid";
    private static final String PROVISION_NIC_MAC = "40:8d:5c:f7:8d:60";
    private static final String DISCOVERED_PROVISION_NIC_MAC = "52:54:00:12:34:56";

    @Test
    public void networkMissingFailsBeforeProviderDispatch() throws Exception {
        Harness h = new Harness().withServer(validServer()).withoutNetwork();

        Result result = h.start();

        result.assertFailedWith("ProvisionNetwork[uuid:network-uuid] not found");
        Assert.assertFalse(h.provider.invoked);
    }

    @Test
    public void disabledNetworkFailsBeforeProviderDispatch() throws Exception {
        PhysicalServerProvisionNetworkVO network = validNetwork();
        network.setState(ProvisionNetworkState.Disabled);
        Harness h = new Harness().withServer(validServer()).withNetwork(network);

        Result result = h.start();

        result.assertFailedWith("not Enabled");
        Assert.assertFalse(h.provider.invoked);
    }

    @Test
    public void networkZoneMismatchFailsBeforeProviderDispatch() throws Exception {
        PhysicalServerProvisionNetworkVO network = validNetwork();
        network.setZoneUuid("other-zone");
        Harness h = new Harness().withServer(validServer()).withNetwork(network);

        Result result = h.start();

        result.assertFailedWith("belongs to Zone[uuid:other-zone]");
        Assert.assertFalse(h.provider.invoked);
    }

    @Test
    public void serverWithoutPoolFailsBeforeProviderDispatch() throws Exception {
        PhysicalServerVO server = validServer();
        server.setPoolUuid(null);
        Harness h = new Harness().withServer(server).withNetwork(validNetwork());

        Result result = h.start();

        result.assertFailedWith("not assigned to any ServerPool");
        Assert.assertFalse(h.provider.invoked);
    }

    @Test
    public void networkNotAttachedToServerPoolFailsBeforeProviderDispatch() throws Exception {
        Harness h = new Harness()
                .withServer(validServer())
                .withNetwork(validNetwork())
                .withPoolRef(false);

        Result result = h.start();

        result.assertFailedWith("is not attached to PhysicalServer");
        Assert.assertFalse(h.provider.invoked);
    }

    @Test
    public void pxeProvisionWithoutOobCredentialsFailsBeforeProviderDispatch() throws Exception {
        PhysicalServerVO server = validServer();
        server.setOobAddress(null);
        server.setOobUsername(null);
        server.setOobPassword(null);
        Harness h = new Harness()
                .withServer(server)
                .withNetwork(validNetwork())
                .withPoolRef(true);

        Result result = h.start();

        result.assertFailedWith("has no OOB/IPMI credentials for PXE provision");
        Assert.assertFalse(h.provider.invoked);
    }

    @Test
    public void provisionNicMacMustExistInDiscoveredHardware() throws Exception {
        Harness h = new Harness()
                .withServer(validServer())
                .withNetwork(validNetwork())
                .withPoolRef(true)
                .withProvisionNic(false);

        Result result = h.start();

        result.assertFailedWith("provision NIC");
        result.assertFailedWith(PROVISION_NIC_MAC);
        Assert.assertFalse(h.provider.invoked);
    }

    @Test
    public void providerReceivesPhysicalServerTargetFromRequestMac() throws Exception {
        Harness h = new Harness()
                .withServer(validServer())
                .withNetwork(validNetwork())
                .withPoolRef(true)
                .withProvisionNic(true);

        Result result = h.start();

        result.assertSucceeded();
        PhysicalServerProvisionTarget target = h.provider.request.getTarget();
        Assert.assertEquals(SERVER_UUID, target.getServerUuid());
        Assert.assertEquals(NETWORK_UUID, target.getNetworkUuid());
        Assert.assertEquals("192.168.63.10", target.getManagementIp());
        Assert.assertEquals("192.168.63.20", target.getOobAddress());
        Assert.assertEquals(Integer.valueOf(623), target.getOobPort());
        Assert.assertEquals("admin", target.getOobUsername());
        Assert.assertEquals("password", target.getOobPassword());
        Assert.assertEquals(PROVISION_NIC_MAC, target.getProvisionNicMac());
        Assert.assertEquals("eth0", target.getDhcpInterface());
        Assert.assertEquals("192.168.0.10", target.getDhcpRangeStartIp());
        Assert.assertEquals("192.168.0.100", target.getDhcpRangeEndIp());
        Assert.assertEquals("255.255.255.0", target.getDhcpRangeNetmask());
        Assert.assertEquals("192.168.0.1", target.getDhcpRangeGateway());
        Assert.assertEquals("image-uuid", target.getOsImageUuid());
        Assert.assertEquals("rocky9", target.getOsDistribution());
        Assert.assertEquals("kickstart", target.getKickstartTemplate());
        Assert.assertEquals("value", target.getCustomParams().get("key"));
    }

    @Test
    public void targetFallsBackToDiscoveredPrimaryProvisionNicWhenRequestMacAbsent() throws Exception {
        Harness h = new Harness()
                .withServer(validServer())
                .withNetwork(validNetwork())
                .withoutRequestedProvisionNic()
                .withPoolRef(true)
                .withDiscoveredProvisionNic(DISCOVERED_PROVISION_NIC_MAC);

        Result result = h.start();

        result.assertSucceeded();
        Assert.assertEquals(DISCOVERED_PROVISION_NIC_MAC, h.provider.request.getTarget().getProvisionNicMac());
    }

    @Test
    public void physicalServerTargetDoesNotExposeBm2IdentityFields() {
        for (Field field : PhysicalServerProvisionTarget.class.getDeclaredFields()) {
            String name = field.getName().toLowerCase();
            Assert.assertFalse(name.equals("chassisuuid"));
            Assert.assertFalse(name.equals("gatewayuuid"));
            Assert.assertFalse(name.equals("bminstanceuuid"));
            Assert.assertFalse(name.equals("chassisofferinguuid"));
        }
    }

    private static PhysicalServerVO validServer() {
        PhysicalServerVO server = new PhysicalServerVO();
        server.setUuid(SERVER_UUID);
        server.setZoneUuid(ZONE_UUID);
        server.setPoolUuid(POOL_UUID);
        server.setManagementIp("192.168.63.10");
        server.setOobManagementType("IPMI");
        server.setOobAddress("192.168.63.20");
        server.setOobPort(623);
        server.setOobUsername("admin");
        server.setOobPassword("password");
        return server;
    }

    private static PhysicalServerProvisionNetworkVO validNetwork() {
        PhysicalServerProvisionNetworkVO network = new PhysicalServerProvisionNetworkVO();
        network.setUuid(NETWORK_UUID);
        network.setZoneUuid(ZONE_UUID);
        network.setType(ProvisionNetworkType.GATEWAY_PXE);
        network.setState(ProvisionNetworkState.Enabled);
        network.setDhcpInterface("eth0");
        network.setDhcpRangeStartIp("192.168.0.10");
        network.setDhcpRangeEndIp("192.168.0.100");
        network.setDhcpRangeNetmask("255.255.255.0");
        network.setDhcpRangeGateway("192.168.0.1");
        return network;
    }

    private static APIProvisionPhysicalServerMsg validMsg() {
        APIProvisionPhysicalServerMsg msg = new APIProvisionPhysicalServerMsg();
        msg.setServerUuid(SERVER_UUID);
        msg.setNetworkUuid(NETWORK_UUID);
        msg.setOsImageUuid("image-uuid");
        msg.setOsDistribution("rocky9");
        msg.setKickstartTemplate("kickstart");
        msg.setProvisionNicMac(PROVISION_NIC_MAC);
        Map<String, String> customParams = new HashMap<>();
        customParams.put("key", "value");
        msg.setCustomParams(customParams);
        return msg;
    }

    private static class Harness {
        private PhysicalServerVO server;
        private PhysicalServerProvisionNetworkVO network;
        private Boolean poolRefExists;
        private Boolean provisionNicExists;
        private String discoveredProvisionNicMac;
        private APIProvisionPhysicalServerMsg msg = validMsg();
        private final RecordingProvider provider = new RecordingProvider();

        Harness withServer(PhysicalServerVO server) {
            this.server = server;
            return this;
        }

        Harness withNetwork(PhysicalServerProvisionNetworkVO network) {
            this.network = network;
            return this;
        }

        Harness withoutNetwork() {
            this.network = null;
            return this;
        }

        Harness withPoolRef(boolean exists) {
            this.poolRefExists = exists;
            return this;
        }

        Harness withProvisionNic(boolean exists) {
            this.provisionNicExists = exists;
            return this;
        }

        Harness withoutRequestedProvisionNic() {
            msg.setProvisionNicMac(null);
            return this;
        }

        Harness withDiscoveredProvisionNic(String mac) {
            this.discoveredProvisionNicMac = mac;
            return this;
        }

        Result start() throws Exception {
            PhysicalServerProvisionService service = new PhysicalServerProvisionService();
            DatabaseFacade dbf = mock(DatabaseFacade.class);
            doAnswer(invocation -> {
                String uuid = invocation.getArgument(0);
                Class<?> type = invocation.getArgument(1);
                if (type == PhysicalServerVO.class && SERVER_UUID.equals(uuid)) {
                    return server;
                }
                if (type == PhysicalServerProvisionNetworkVO.class && NETWORK_UUID.equals(uuid)) {
                    return network;
                }
                return null;
            }).when(dbf).findByUuid(eq(SERVER_UUID), eq(PhysicalServerVO.class));
            doAnswer(invocation -> network).when(dbf).findByUuid(eq(NETWORK_UUID), eq(PhysicalServerProvisionNetworkVO.class));
            inject(service, "dbf", dbf);
            inject(service, "providerList", Collections.singletonList(provider));

            Result result = new Result();
            try (MockedStatic<Q> q = mockStatic(Q.class);
                 MockedStatic<Platform> platform = mockStatic(Platform.class)) {
                stubOperr(platform);
                if (poolRefExists != null) {
                    Q poolRefQuery = queryExists(poolRefExists);
                    q.when(() -> Q.New(PhysicalServerProvisionNetworkPoolRefVO.class))
                            .thenReturn(poolRefQuery);
                }
                if (provisionNicExists != null) {
                    Q provisionNicQuery = queryExists(provisionNicExists);
                    q.when(() -> Q.New(PhysicalServerHardwareDetailVO.class))
                            .thenReturn(provisionNicQuery);
                }
                if (discoveredProvisionNicMac != null) {
                    Q provisionNicQuery = queryDetail(nicDetail(discoveredProvisionNicMac));
                    q.when(() -> Q.New(PhysicalServerHardwareDetailVO.class))
                            .thenReturn(provisionNicQuery);
                }

                ReturnValueCompletion<ProvisionResult> completion = mock(ReturnValueCompletion.class);
                doAnswer(invocation -> {
                    result.value = invocation.getArgument(0);
                    return null;
                }).when(completion).success(any(ProvisionResult.class));
                doAnswer(invocation -> {
                    result.error = invocation.getArgument(0);
                    return null;
                }).when(completion).fail(any(ErrorCode.class));
                service.startProvisioning(msg, ACCOUNT_UUID, "test-job-uuid",
                        org.zstack.header.server.ProvisionPhase.NotStarted, completion);
            }
            return result;
        }

        private static void stubOperr(MockedStatic<Platform> platform) {
            platform.when(() -> Platform.operr(anyString(), anyString(), any()))
                    .thenAnswer(invocation -> errorFromOperrInvocation(invocation.getArguments()));
            platform.when(() -> Platform.operr(anyString(), anyString(), any(), any(), any()))
                    .thenAnswer(invocation -> errorFromOperrInvocation(invocation.getArguments()));
            platform.when(() -> Platform.operr(anyString(), anyString(), any(), any(), any(), any()))
                    .thenAnswer(invocation -> errorFromOperrInvocation(invocation.getArguments()));
        }

        private static ErrorCode errorFromOperrInvocation(Object[] arguments) {
            String globalErrorCode = (String) arguments[0];
            String format = (String) arguments[1];
            Object[] formatArgs = Arrays.copyOfRange(arguments, 2, arguments.length);
            return new ErrorCode(globalErrorCode, "operation error", String.format(format, formatArgs));
        }

        private static Q queryExists(boolean exists) {
            Q query = mock(Q.class);
            when(query.eq(any(), any())).thenReturn(query);
            when(query.like(any(), any())).thenReturn(query);
            when(query.isExists()).thenReturn(exists);
            return query;
        }

        private static Q queryDetail(PhysicalServerHardwareDetailVO detail) {
            Q query = queryExists(true);
            when(query.list()).thenReturn(Collections.singletonList(detail));
            return query;
        }

        private static PhysicalServerHardwareDetailVO nicDetail(String mac) {
            PhysicalServerHardwareDetailVO detail = new PhysicalServerHardwareDetailVO();
            detail.setServerUuid(SERVER_UUID);
            detail.setType("NIC");
            detail.setExtraInfo(String.format("{\"mac\":\"%s\",\"primary\":true}", mac));
            return detail;
        }

        private static void inject(Object target, String fieldName, Object value) throws Exception {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        }
    }

    private static class RecordingProvider implements ProvisionProvider {
        boolean invoked;
        ProvisionRequest request;

        @Override
        public ProvisionNetworkType getType() {
            return ProvisionNetworkType.GATEWAY_PXE;
        }

        @Override
        public void prepareNetwork(PhysicalServerProvisionNetworkInventory network, String poolUuid, Completion completion) {
            completion.success();
        }

        @Override
        public void destroyNetwork(PhysicalServerProvisionNetworkInventory network, String poolUuid, Completion completion) {
            completion.success();
        }

        @Override
        public void startProvisioning(ProvisionRequest request, ReturnValueCompletion<ProvisionResult> completion) {
            invoked = true;
            this.request = request;
            completion.success(new ProvisionResult()
                    .setServerUuid(request.getServerUuid())
                    .setNetworkUuid(request.getNetworkUuid())
                    .setProviderType(getType().toString()));
        }
    }

    private static class Result {
        private ProvisionResult value;
        private ErrorCode error;

        void assertFailedWith(String message) {
            Assert.assertNull("provision should not succeed", value);
            Assert.assertNotNull("expected validation failure containing: " + message, error);
            Assert.assertTrue("expected error to contain [" + message + "] but was: " + error,
                    error.toString().contains(message));
        }

        void assertSucceeded() {
            Assert.assertNotNull("provision should succeed", value);
            Assert.assertNull("provision should not fail", error);
        }
    }
}
