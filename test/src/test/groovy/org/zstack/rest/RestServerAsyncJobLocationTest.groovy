package org.zstack.rest

import org.junit.Test
import org.zstack.core.rest.RESTFacadeImpl
import org.zstack.header.rest.RESTFacade

import javax.servlet.http.HttpServletRequest

class RestServerAsyncJobLocationTest {
    private static final String JOB_UUID = "7d53fcaefc3f49f1b20866f84e551737"

    @Test
    void testRequestInfoKeepsTheAddressUsedToReachTheApi() {
        HttpServletRequest request = request("2001:db8::10", "2001:db8::20", [:])

        RestServer.RequestInfo info = new RestServer.RequestInfo(request)

        assert info.clientIp == "2001:db8::10"
        assert info.requestDestinationAddress == "2001:db8::20"
    }

    @Test
    void testForwardedRequestUsesConfiguredPublicBaseUrl() {
        HttpServletRequest request = request("127.0.0.1", "127.0.0.1",
                ["X-Forwarded-Host": "api.example.com"])

        RestServer.RequestInfo info = new RestServer.RequestInfo(request)

        assert info.requestDestinationAddress == null
    }

    @Test
    void testAsyncJobLocationUsesTheAddressUsedToReachTheApi() {
        RESTFacade restf = [
                buildBaseUrl: { String host -> RESTFacadeImpl.buildBaseUrl(host, 8080, "zstack") },
                getBaseUrl  : { "http://192.168.10.10:8080/zstack" },
        ] as RESTFacade

        assert RestServer.buildAsyncJobLocation(JOB_UUID, "192.168.10.20", restf) ==
                "http://192.168.10.20:8080/zstack/v1/api-jobs/${JOB_UUID}"
        assert RestServer.buildAsyncJobLocation(JOB_UUID, "2001:db8::20", restf) ==
                "http://[2001:db8::20]:8080/zstack/v1/api-jobs/${JOB_UUID}"
        assert RestServer.buildAsyncJobLocation(JOB_UUID, null, restf) ==
                "http://192.168.10.10:8080/zstack/v1/api-jobs/${JOB_UUID}"
    }

    private static HttpServletRequest request(String remoteAddress, String localAddress,
                                              Map<String, String> headers) {
        return [
                getSession   : { null },
                getRemoteHost: { remoteAddress },
                getRemoteAddr: { remoteAddress },
                getLocalAddr : { localAddress },
                getHeader    : { String name -> headers[name] },
                getHeaderNames: { Collections.enumeration(headers.keySet()) },
                getRequestURI: { "/zstack/v1/consoles" },
                getMethod    : { "POST" },
        ] as HttpServletRequest
    }
}
