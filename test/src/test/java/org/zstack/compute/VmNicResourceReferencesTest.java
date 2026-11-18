package org.zstack.compute;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.zstack.compute.vm.VmInstanceApiInterceptor;
import org.zstack.core.Platform;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.vm.*;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class VmNicResourceReferencesTest {
    @Test
    public void extractsAndDeduplicatesNetworksFromTypedAndJsonParams() {
        VmNicParam nic = new VmNicParam();
        nic.setL3NetworkUuid("private-l3");
        Map<Class<?>, List<String>> expected = Collections.singletonMap(L3NetworkVO.class, Arrays.asList("private-l3"));
        assertEquals(expected, VmNicUtils.getNetworkReferences(Arrays.asList(nic, nic)));
        assertEquals(expected, VmNicUtils.getNetworkReferences("[{\"l3NetworkUuid\":\"private-l3\"}]", false));
        assertEquals(expected, VmNicUtils.getNetworkReferences("{\"l3NetworkUuid\":\"private-l3\"}", true));
        assertTrue(VmNicUtils.getNetworkReferences((List<VmNicParam>) null).isEmpty());
        assertTrue(VmNicUtils.getNetworkReferences(Collections.emptyList()).isEmpty());
        assertTrue(VmNicUtils.getNetworkReferences(null, false).isEmpty());
        assertTrue(VmNicUtils.getNetworkReferences("", true).isEmpty());
    }

    @Test
    public void malformedParamsCannotSilentlySkipAuthorization() {
        try (MockedStatic<Platform> platform = mockStatic(Platform.class, invocation -> new ErrorCode())) {
            for (String json : Arrays.asList("{", "{}", "null", "[null]", "[{}]", "[{\"l3NetworkUuid\":\" \"}]")) {
                assertThrows(json, ApiMessageInterceptionException.class, () -> VmNicUtils.getNetworkReferences(json, false));
            }
            assertThrows(ApiMessageInterceptionException.class, () -> VmNicUtils.getNetworkReferences("[]", true));
            assertThrows(ApiMessageInterceptionException.class, () -> VmNicUtils.getNetworkReferences("null", true));
        }
    }

    @Test
    public void createAndAttachApisDeclareTheirNestedNetworks() {
        VmInstanceApiInterceptor interceptor = new VmInstanceApiInterceptor();
        APICreateVmInstanceMsg create = new APICreateVmInstanceMsg();
        create.setVmNicParams("[{\"l3NetworkUuid\":\"nested-l3\"}]");
        APIAttachL3NetworkToVmMsg attach = new APIAttachL3NetworkToVmMsg();
        attach.setVmNicParams("{\"l3NetworkUuid\":\"nested-l3\"}");
        Map<Class<?>, List<String>> expected = Collections.singletonMap(L3NetworkVO.class, Arrays.asList("nested-l3"));
        assertEquals(expected, interceptor.getResourceReferences(create));
        assertEquals(expected, interceptor.getResourceReferences(attach));
        assertTrue(interceptor.getResourceReferences(new APIMessage() {}).isEmpty());
    }
}
