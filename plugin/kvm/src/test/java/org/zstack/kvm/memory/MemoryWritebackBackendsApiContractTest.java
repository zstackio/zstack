package org.zstack.kvm.memory;

import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.rest.RestResponse;
import org.zstack.rest.RestServer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class MemoryWritebackBackendsApiContractTest {
    @Test
    public void declaresHostReadGetRouteAndInventoryResponse() throws Exception {
        RestRequest request = APIGetHostMemoryWritebackBackendsMsg.class.getAnnotation(RestRequest.class);
        assertNotNull(request);
        assertEquals("/hosts/{hostUuid}/memory-writeback-backends", request.path());
        assertEquals(HttpMethod.GET, request.method());
        assertEquals(APIGetHostMemoryWritebackBackendsReply.class, request.responseClass());

        Action action = APIGetHostMemoryWritebackBackendsMsg.class.getAnnotation(Action.class);
        assertNotNull(action);
        assertEquals(HostConstant.ACTION_CATEGORY, action.category());
        assertArrayEquals(new String[]{"read"}, action.names());

        Field hostUuid = APIGetHostMemoryWritebackBackendsMsg.class.getDeclaredField("hostUuid");
        APIParam parameter = hostUuid.getAnnotation(APIParam.class);
        assertTrue(parameter.required());
        assertEquals(HostVO.class, parameter.resourceType());
        assertArrayEquals(new String[]{"inventory"}, APIGetHostMemoryWritebackBackendsReply.class
                .getAnnotation(RestResponse.class).fieldsTo());
        assertEquals(MemoryWritebackBackendInventory.class,
                APIGetHostMemoryWritebackBackendsReply.class.getMethod("getInventory").getReturnType());
    }

    @Test
    public void routePassesFrameworkRegistrationValidationAndMemoryServiceRouting() throws Exception {
        Method validate = RestServer.class.getDeclaredMethod("collectRestRequestErrConfigApi",
                List.class, Class.class, RestRequest.class);
        validate.setAccessible(true);
        List<String> errors = new ArrayList<>();
        validate.invoke(new RestServer(), errors, APIGetHostMemoryWritebackBackendsMsg.class,
                APIGetHostMemoryWritebackBackendsMsg.class.getAnnotation(RestRequest.class));
        assertTrue("RestServer rejected writeback backend route: " + errors, errors.isEmpty());

        Path config = null;
        for (Path parent = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
             parent != null; parent = parent.getParent()) {
            Path path = parent.resolve("conf/serviceConfig/memoryOptimization.xml");
            if (Files.isRegularFile(path)) { config = path; break; }
        }
        assertNotNull("memoryOptimization service config is not available", config);
        String xml = new String(Files.readAllBytes(config), StandardCharsets.UTF_8);
        assertTrue(xml.contains("org.zstack.kvm.memory.APIGetHostMemoryWritebackBackendsMsg"));
    }

    @Test public void exampleRetainsObservedPreparedDeviceAndPublishedUiFields() {
        MemoryWritebackBackendInventory inventory = APIGetHostMemoryWritebackBackendsReply.__example__().getInventory();
        assertEquals("APPLIED", inventory.getMaintenance().getStage());
        assertEquals("288097e635094a56afe759757cc5397f", inventory.getMaintenance().getRequest_id());
        assertEquals("6266ba305b5ed487b5f22cefee13f466", inventory.getCandidates().get(0).getResourceUuid());
        assertEquals(Long.valueOf(21474836480L), inventory.getCandidates().get(0).getCapacityBytes());
        String json = new com.google.gson.Gson().toJson(inventory);
        assertTrue(json.contains("\"writebackReady\":true"));
        assertTrue(json.contains("\"capacityBytes\":21474836480"));
        assertTrue(json.contains("\"request_id\":\"288097e635094a56afe759757cc5397f\""));
    }
}
