package org.zstack.console;

import org.junit.Assert;
import org.junit.Test;
import org.zstack.core.rest.RESTFacadeImpl;
import org.zstack.header.console.ConsoleConstants;
import org.zstack.header.console.ConsoleProxyInventory;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.rest.RESTConstant;
import org.zstack.header.rest.RESTFacade;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TestConsoleProxyLocalAgentCallback {
    @Test
    public void testAsyncCallsUseCurrentManagementNodeCallback() throws Exception {
        List<Object[]> requests = new ArrayList<>();
        RESTFacade restf = (RESTFacade) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{RESTFacade.class}, (proxy, method, args) -> {
                    if ("getCallbackUrl".equals(method.getName())) {
                        return "http://[2001:db8::1]:8080/zstack/asyncrest/callback";
                    }
                    if ("asyncJsonPost".equals(method.getName()) && args.length == 4) {
                        requests.add(args);
                    }
                    return null;
                });

        ConsoleProxyInventory inventory = new ConsoleProxyInventory();
        inventory.setAgentIp("127.0.0.1");
        ConsoleProxyBase proxy = new ConsoleProxyBase(inventory, 7758);
        Field restFacadeField = ConsoleProxyBase.class.getDeclaredField("restf");
        restFacadeField.setAccessible(true);
        restFacadeField.set(proxy, restf);

        List<String> paths = Arrays.asList(ConsoleConstants.CONSOLE_PROXY_ESTABLISH_PROXY_PATH,
                ConsoleConstants.CONSOLE_PROXY_CHECK_PROXY_PATH,
                ConsoleConstants.CONSOLE_PROXY_DELETE_PROXY_PATH);
        for (String path : paths) {
            proxy.asyncJsonPostToLocalConsoleAgent(path, new Object(), null);
        }

        Assert.assertEquals(paths.size(), requests.size());
        for (int i = 0; i < paths.size(); i++) {
            Object[] request = requests.get(i);
            Assert.assertEquals("http://127.0.0.1:7758" + paths.get(i), request[0]);
            Map<String, String> headers = (Map<String, String>) request[2];
            Assert.assertEquals("http://[2001:db8::1]:8080/zstack/asyncrest/callback",
                    headers.get(RESTConstant.CALLBACK_URL));
        }
    }

    @Test
    public void testIpv4LocalAgentUsesSuppliedIpv6Callback() {
        String callbackUrl = "http://[2001:db8::1]:8080/zstack/asyncrest/callback";
        ErrorableValue<String> selectedCallbackUrl = RESTFacadeImpl.selectCallbackUrl(
                "http://127.0.0.1:7758" + ConsoleConstants.CONSOLE_PROXY_ESTABLISH_PROXY_PATH,
                Collections.singletonMap(RESTConstant.CALLBACK_URL, callbackUrl), callbackUrl, 8080, "zstack");

        Assert.assertTrue(selectedCallbackUrl.isSuccess());
        Assert.assertEquals(callbackUrl, selectedCallbackUrl.result);
    }
}
